import {computed, nextTick, ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {
    createComment,
    getCommentReplies,
    getComments,
    getFollowing,
    likeComment as likeCommentApi,
    unlikeComment as unlikeCommentApi
} from '@/api/inspire.js'

export function useInspireComments({detail, isLogin, requireLogin}) {
  let commentRequestSeq = 0

  const comments = ref([])
  const allCommentRecords = ref([])
  const commentPage = ref(1)
  const commentTotal = ref(0)
  const commentSort = ref('hot')
  const commentLoading = ref(false)
  const commentError = ref('')
  const quickCommentText = ref('')
  const replyText = ref('')
  const replyTarget = ref(null)
  const replyToUser = ref(null)
  const submittingComment = ref(false)
  const mentionOpen = ref(false)
  const mentionLoading = ref(false)
  const mentionLoaded = ref(false)
  const mentionTarget = ref('')
  const mentionStart = ref(0)
  const mentionEnd = ref(0)
  const mentionQuery = ref('')
  const mentionUsers = ref([])

  const displayComments = computed(() => comments.value)
  const commentState = computed(() => {
    if (commentLoading.value && !comments.value.length) return 'loading'
    if (commentError.value && !comments.value.length) return 'error'
    return comments.value.length ? 'ready' : 'empty'
  })
  const loadedCommentCount = computed(() => comments.value.length)
  const commentHasMore = computed(() => !commentLoading.value && comments.value.length < commentTotal.value)
  const mentionCandidates = computed(() => {
    const keyword = mentionQuery.value.trim().toLowerCase()
    if (!keyword) return mentionUsers.value.slice(0, 10)
    return mentionUsers.value.filter(user => {
      const nickname = String(user.nickname || '').toLowerCase()
      return nickname.includes(keyword)
    }).slice(0, 10)
  })

  const formatRelativeTime = (value) => {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    const diffMin = Math.floor((Date.now() - date.getTime()) / 60000)
    if (diffMin < 5) return '刚刚'
    if (diffMin < 60) return `${diffMin}分钟前`
    const diffHour = Math.floor(diffMin / 60)
    if (diffHour < 24) return `${diffHour}小时前`
    const month = date.getMonth() + 1
    const day = date.getDate()
    if (date.getFullYear() === new Date().getFullYear()) return `${month}月${day}日`
    return `${date.getFullYear()}年${month}月${day}日`
  }

  const ensureMentionUsers = async () => {
    if (mentionLoaded.value || mentionLoading.value) return
    mentionLoading.value = true
    try {
      const res = await getFollowing()
      mentionUsers.value = res.data || []
      mentionLoaded.value = true
    } catch (e) {
      mentionUsers.value = []
    } finally {
      mentionLoading.value = false
    }
  }

  const closeMention = () => {
    mentionOpen.value = false
    mentionTarget.value = ''
    mentionQuery.value = ''
  }

  const onMentionInput = (event, target) => {
    const value = target === 'reply' ? replyText.value : quickCommentText.value
    const cursor = event.target.selectionStart ?? value.length
    const before = value.slice(0, cursor)
    const match = before.match(/@([^@\s]*)$/)
    if (!match) {
      closeMention()
      return
    }
    mentionOpen.value = true
    mentionTarget.value = target
    mentionStart.value = cursor - match[0].length
    mentionEnd.value = cursor
    mentionQuery.value = match[1]
    ensureMentionUsers()
  }

  const selectMention = (user) => {
    const target = mentionTarget.value
    const value = target === 'reply' ? replyText.value : quickCommentText.value
    const token = `@${user.nickname || '灵感用户'} `
    const next = value.slice(0, mentionStart.value) + token + value.slice(mentionEnd.value)
    if (target === 'reply') replyText.value = next
    else quickCommentText.value = next
    closeMention()
    nextTick(() => {
      const selector = target === 'reply' ? '.reply-box input' : '.quick-comment input'
      document.querySelector(selector)?.focus()
    })
  }

  const formatCommentTime = (value) => {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    const now = new Date()
    const diffMin = Math.floor((now.getTime() - date.getTime()) / 60000)
    if (diffMin >= 0 && diffMin < 5) return '刚刚'
    if (diffMin >= 0 && diffMin < 60) return `${diffMin}分钟前`
    const diffHour = Math.floor(diffMin / 60)
    if (diffHour >= 0 && diffHour < 24 && date.toDateString() === now.toDateString()) {
      return `${diffHour}小时前`
    }
    const month = date.getMonth() + 1
    const day = date.getDate()
    const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    if (date.getFullYear() === now.getFullYear()) return `${month}月${day}日 ${time}`
    return `${date.getFullYear()}年${month}月${day}日 ${time}`
  }

  const sortCommentRecords = (records) => records.slice().sort((a, b) => {
    if (commentSort.value === 'hot') {
      const likeDiff = Number(b.likeCount || 0) - Number(a.likeCount || 0)
      if (likeDiff !== 0) return likeDiff
    }
    const left = new Date(a.createTime || 0).getTime()
    const right = new Date(b.createTime || 0).getTime()
    return right - left
  })

  const regroupComments = () => {
    const previousState = new Map()
    comments.value.forEach(item => {
      previousState.set(String(item.id), {
        expanded: item._repliesExpanded,
        visibleCount: item._visibleReplyCount
      })
    })

    const records = sortCommentRecords(allCommentRecords.value)
    const recordMap = new Map(records.map(item => [String(item.id), item]))
    const rootIds = new Set(
      records
        .filter(item => !item.parentId || String(item.parentId) === '0')
        .map(item => String(item.id))
    )

    const resolveRootId = (item) => {
      let parentId = String(item.parentId || '0')
      let guard = 0
      while (parentId !== '0' && recordMap.has(parentId) && guard < 12) {
        const parent = recordMap.get(parentId)
        if (!parent.parentId || String(parent.parentId) === '0') return parentId
        parentId = String(parent.parentId)
        guard += 1
      }
      return parentId
    }

    const roots = []
    const replyMap = new Map()
    records.forEach(item => {
      if (!item.parentId || String(item.parentId) === '0') {
        roots.push(item)
        return
      }
      const rootId = resolveRootId(item)
      if (!rootIds.has(rootId)) return
      if (!replyMap.has(rootId)) replyMap.set(rootId, [])
      replyMap.get(rootId).push(item)
    })

    roots.forEach(root => {
      const replies = sortCommentRecords(replyMap.get(String(root.id)) || [])
      const previous = previousState.get(String(root.id))
      root.replies = replies
      root._repliesExpanded = previous?.expanded || false
      root._visibleReplyCount = previous?.visibleCount || 0
      replies.forEach(reply => { reply._avatarErr = false })
    })

    comments.value = roots
  }

  const loadComments = async (reset = false, inspireIdOverride = null) => {
    const inspireId = String(inspireIdOverride || detail.value.id || '')
    if (!inspireId || commentLoading.value) return
    if (!isLogin.value) {
      comments.value = []
      allCommentRecords.value = []
      commentTotal.value = 0
      return
    }
    const requestSeq = ++commentRequestSeq
    commentLoading.value = true
    commentError.value = ''
    const targetPage = reset ? 1 : commentPage.value
    try {
      const res = await getComments(inspireId, {
        page: targetPage,
        size: 20,
        sort: commentSort.value
      })
      if (requestSeq !== commentRequestSeq
          || (!inspireIdOverride && String(detail.value.id) !== inspireId)) return
      if (res.code !== 200) return
      const data = res.data || {}
      const records = Array.isArray(data) ? data : (data.records || [])
      const source = reset ? records : [...allCommentRecords.value, ...records]
      const merged = new Map(source.map(item => [String(item.id), item]))
      allCommentRecords.value = sortCommentRecords([...merged.values()])
      commentTotal.value = Number(data.total ?? allCommentRecords.value.length)
      commentPage.value = targetPage + 1
      regroupComments()
    } catch (e) {
      if (requestSeq !== commentRequestSeq
          || (!inspireIdOverride && String(detail.value.id) !== inspireId)) return
      commentError.value = e?.message || 'load comments failed'
      ElMessage.error('评论加载失败')
    } finally {
      if (requestSeq === commentRequestSeq) {
        commentLoading.value = false
      }
    }
  }

  const visibleReplies = (commentItem) => {
    if (!commentItem.replies?.length) return []
    if (!commentItem._repliesExpanded) return []
    const count = Math.min(commentItem._visibleReplyCount || 3, commentItem.replies.length)
    return commentItem.replies.slice(0, count)
  }

  const expandReplies = (commentItem) => {
    commentItem._repliesExpanded = true
    commentItem._visibleReplyCount = Math.min(3, commentItem.replies.length)
    if ((commentItem.replies?.length || 0) < Number(commentItem.replyCount || 0)) {
      loadMoreReplies(commentItem)
    }
  }

  const loadMoreReplies = (commentItem) => {
    if (commentItem._repliesLoading) return Promise.resolve()
    commentItem._repliesLoading = true
    const nextPage = commentItem._replyPage || 1
    return getCommentReplies(detail.value.id, commentItem.id, {
      page: nextPage,
      size: 20,
      sort: commentSort.value
    }).then(res => {
      if (res.code !== 200) throw new Error(res.msg || '回复加载失败')
      const rows = res.data?.records || []
      const merged = new Map(allCommentRecords.value.map(item => [String(item.id), item]))
      rows.forEach(item => merged.set(String(item.id), item))
      allCommentRecords.value = sortCommentRecords([...merged.values()])
      const previousPage = commentItem._replyPage || 1
      regroupComments()
      const root = comments.value.find(item => String(item.id) === String(commentItem.id))
      if (root) {
        root._repliesExpanded = true
        root._visibleReplyCount = root.replies?.length || 0
        root._replyPage = previousPage + 1
        root._repliesLoading = false
        root.replyCount = Number(res.data?.total || root.replyCount || root.replies?.length || 0)
      }
    }).catch(() => {
      ElMessage.error('回复加载失败')
      commentItem._repliesLoading = false
    })
  }

  const collapseReplies = (commentItem) => {
    commentItem._repliesExpanded = false
    commentItem._visibleReplyCount = 0
  }

  const replyTo = (commentItem, userInfo = null) => {
    closeMention()
    if (!isLogin.value) { requireLogin(); return }
    let root = commentItem
    if (userInfo) {
      root = comments.value.find(item => item.replies?.some(reply => String(reply.id) === String(commentItem.id))) || commentItem
    }
    replyTarget.value = root
    replyToUser.value = userInfo
    replyText.value = ''
    nextTick(() => document.querySelector('.reply-box input')?.focus())
  }

  const cancelReply = () => {
    closeMention()
    replyTarget.value = null
    replyToUser.value = null
    replyText.value = ''
  }

  /**
   * 评论列表按热度分页时，新评论/新回复的点赞数最低，可能不在第一页。
   * 服务端创建成功后直接合并返回的记录，保证用户马上能看到自己的内容。
   */
  const mergeCreatedComment = (created) => {
    if (!created?.id) return null
    const existed = allCommentRecords.value.some(item => String(item.id) === String(created.id))
    const isRootComment = !created.parentId || String(created.parentId) === '0'
    const record = { ...created, _avatarErr: false }
    const next = new Map(allCommentRecords.value.map(item => [String(item.id), item]))
    next.set(String(record.id), record)
    allCommentRecords.value = sortCommentRecords([...next.values()])
    if (!existed && isRootComment) commentTotal.value += 1
    regroupComments()

    const parentId = String(record.parentId || '0')
    const root = parentId === '0'
      ? comments.value.find(item => String(item.id) === String(record.id))
      : comments.value.find(item =>
          String(item.id) === parentId
          || item.replies?.some(reply => String(reply.id) === String(record.id))
        )
    if (root && parentId !== '0') {
      if (!existed) root.replyCount = Number(root.replyCount || root.replies?.length || 0) + 1
      root._repliesExpanded = true
      root._visibleReplyCount = root.replies?.length || 0
      root._replyPage = Math.max(1, Math.ceil(root.replies.length / 20))
    }
    return { record, root }
  }

  const focusCreatedComment = async (created, root) => {
    await nextTick()
    const targetId = String(created?.id || root?.id || '')
    const target = Array.from(document.querySelectorAll('[data-comment-id]'))
      .find(el => el.dataset.commentId === targetId)
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  const submitComment = async () => {
    if (!isLogin.value) { requireLogin(); return }
    if (!quickCommentText.value.trim() || submittingComment.value) return
    submittingComment.value = true
    try {
      closeMention()
      const res = await createComment(detail.value.id, { content: quickCommentText.value.trim() })
      if (res.code === 200) {
        quickCommentText.value = ''
        ElMessage.success('评论成功')
        const merged = mergeCreatedComment(res.data)
        if (merged?.record) {
          await focusCreatedComment(merged.record, merged.root)
        } else {
          await loadComments(true)
          await nextTick()
          document.querySelector('.comments-title')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      } else {
        ElMessage.error(res.msg || '评论失败')
      }
    } catch (e) {
      ElMessage.error('评论失败')
    } finally {
      submittingComment.value = false
    }
  }

  const submitReply = async (rootItem) => {
    if (!replyText.value.trim() || submittingComment.value) return
    submittingComment.value = true
    try {
      closeMention()
      const res = await createComment(detail.value.id, {
        content: replyText.value.trim(),
        parentId: rootItem.id,
        replyUserId: replyToUser.value?.userId || rootItem.userId,
        replyUsername: replyToUser.value?.nickname || rootItem.nickname || '灵感用户'
      })
      if (res.code === 200) {
        ElMessage.success('回复成功')
        cancelReply()
        const created = res.data
        await loadComments(true)
        let root = comments.value.find(item => String(item.id) === String(rootItem.id))
        if (!root) {
          root = comments.value.find(item =>
            item.replies?.some(reply => String(reply.id) === String(created?.id))
          )
        }
        if (root) {
          root._repliesExpanded = true
          root._visibleReplyCount = Math.min(root.replies?.length || 0, Number(root.replyCount || 0))
          if (created?.id && !root.replies?.some(reply => String(reply.id) === String(created.id))) {
            await loadMoreReplies(root)
            root = comments.value.find(item => String(item.id) === String(root.id)) || root
          }
        }
        await focusCreatedComment(created, root)
      } else {
        ElMessage.error(res.msg || '回复失败')
      }
    } catch (e) {
      ElMessage.error('回复失败')
    } finally {
      submittingComment.value = false
    }
  }

  const handleQuickCommentFocus = () => {
    if (!isLogin.value) requireLogin()
  }

  const loadMoreComments = () => {
    if (commentHasMore.value) loadComments(false)
  }

  const changeCommentSort = (sort) => {
    if (commentSort.value === sort || commentLoading.value) return
    commentSort.value = sort
    commentPage.value = 1
    comments.value = []
    allCommentRecords.value = []
    loadComments(true)
  }

  const toggleCommentSort = () => {
    changeCommentSort(commentSort.value === 'hot' ? 'time' : 'hot')
  }


  const toggleCommentLike = async (commentItem) => {
    if (!isLogin.value) {
      requireLogin()
      return
    }
    const wasLiked = Boolean(commentItem.liked)
    const oldCount = Number(commentItem.likeCount || 0)
    commentItem.liked = !wasLiked
    commentItem.likeCount = Math.max(0, oldCount + (wasLiked ? -1 : 1))
    try {
      const res = wasLiked
        ? await unlikeCommentApi(detail.value.id, commentItem.id)
        : await likeCommentApi(detail.value.id, commentItem.id)
      if (res.code !== 200) throw new Error(res.msg || 'comment like failed')
    } catch (e) {
      commentItem.liked = wasLiked
      commentItem.likeCount = oldCount
      ElMessage.error('评论点赞失败')
    }
  }

  const invalidateComments = () => {
    commentRequestSeq += 1
  }

  const resetComments = () => {
    comments.value = []
    allCommentRecords.value = []
    commentPage.value = 1
    commentTotal.value = 0
    commentSort.value = 'hot'
    commentLoading.value = false
    commentError.value = ''
    quickCommentText.value = ''
    closeMention()
    cancelReply()
  }

  const disposeComments = () => invalidateComments()

  return {
    formatRelativeTime,
    comments,
    commentPage,
    commentTotal,
    commentSort,
    commentLoading,
    commentError,
    quickCommentText,
    replyText,
    replyTarget,
    replyToUser,
    submittingComment,
    mentionOpen,
    mentionLoading,
    mentionTarget,
    mentionQuery,
    mentionUsers,
    displayComments,
    commentState,
    loadedCommentCount,
    commentHasMore,
    mentionCandidates,
    onMentionInput,
    selectMention,
    loadComments,
    visibleReplies,
    expandReplies,
    loadMoreReplies,
    collapseReplies,
    replyTo,
    cancelReply,
    submitComment,
    submitReply,
    handleQuickCommentFocus,
    loadMoreComments,
    toggleCommentSort,
    toggleCommentLike,
    resetComments,
    invalidateComments,
    disposeComments
  }
}
