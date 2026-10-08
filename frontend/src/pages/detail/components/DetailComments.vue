<!--
  文件：frontend/src/pages/detail/components/DetailComments.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
<button v-if="!isLogin" class="comment-gate" type="button" @click="emit('require-login')">
        <span class="comment-gate-title">登录后显示评论</span>
        <span class="comment-gate-desc">登录即可查看全部评论、回复与点赞</span>
        <span class="comment-gate-btn">去登录</span>
      </button>

      <div v-if="isLogin" class="comments-toolbar">
        <div class="comments-title">
          共 <span>{{ commentTotal }}</span> 条评论 ·
          已显示 <span>{{ loadedCommentCount }}</span>/<span>{{ commentTotal }}</span>
        </div>
        <button
          class="comment-sort-toggle"
          type="button"
          :disabled="commentLoading"
          @click="toggleCommentSort"
        >
          {{ commentSort === 'hot' ? '按热度' : '按时间' }}
        </button>
      </div>

      <AppState
        v-if="isLogin"
        :state="commentState"
        :rows="4"
        loading-variant="text"
        empty-icon="💬"
        empty-text="还没有评论"
        error-text="评论加载失败"
        @retry="loadComments(true)"
      >
      <div class="comment-list">
        <div
          v-for="commentItem in displayComments"
          :key="commentItem.id"
          class="comment-root"
          :data-comment-id="commentItem.id"
        >
          <div class="comment-head">
            <span class="comment-avatar">
              <img
                v-if="isImageAvatar(commentItem.avatar) && !commentItem._avatarErr"
                :src="commentItem.avatar"
                alt="头像"
                @error="commentItem._avatarErr = true"
              >
              <span v-else>{{ avatarText(commentItem.avatar, commentItem.nickname) }}</span>
            </span>
            <b>{{ commentItem.nickname || '灵感用户' }}</b>
            <span>{{ commentItem.createTime ? formatCommentTime(commentItem.createTime) : '' }}</span>
          </div>
          <p class="comment-text">{{ commentItem.content }}</p>
          <div class="comment-tools">
            <span class="comment-like" :class="{ liked: commentItem.liked }" @click="toggleCommentLike(commentItem)">
              ♡ {{ commentItem.likeCount ?? 0 }}
            </span>
            <span @click="replyTo(commentItem)">回复</span>
          </div>

          <div class="replies" :class="{ empty: !commentItem.replies?.length }">
            <div
              v-for="reply in visibleReplies(commentItem)"
              :key="reply.id"
              class="reply"
              :data-comment-id="reply.id"
            >
              <div class="comment-head">
                <span class="comment-avatar">
                  <img
                    v-if="isImageAvatar(reply.avatar) && !reply._avatarErr"
                    :src="reply.avatar"
                    alt="头像"
                    @error="reply._avatarErr = true"
                  >
                  <span v-else>{{ avatarText(reply.avatar, reply.nickname) }}</span>
                </span>
                <b>{{ reply.nickname || '灵感用户' }}</b>
                <span>{{ reply.createTime ? formatCommentTime(reply.createTime) : '' }}</span>
              </div>
              <p class="comment-text">
                <span v-if="reply.replyUsername" class="reply-to">@{{ reply.replyUsername }}</span>
                {{ reply.content }}
              </p>
              <div class="comment-tools">
                <span class="comment-like" :class="{ liked: reply.liked }" @click="toggleCommentLike(reply)">
                  ♡ {{ reply.likeCount ?? 0 }}
                </span>
                <span @click="replyTo(reply, { userId: reply.userId, nickname: reply.nickname || '灵感用户' })">回复</span>
              </div>
            </div>

            <div v-if="commentItem.replyCount" class="reply-actions">
              <button
                v-if="!commentItem._repliesExpanded"
                class="reply-toggle"
                type="button"
                @click="expandReplies(commentItem)"
              >
                展开 {{ commentItem.replyCount }} 条回复
              </button>
              <template v-else>
                <button
                  v-if="commentItem._visibleReplyCount < commentItem.replyCount"
                  class="reply-more"
                  type="button"
                  :disabled="commentItem._repliesLoading"
                  @click="loadMoreReplies(commentItem)"
                >
                  {{ commentItem._repliesLoading
                    ? '加载中...'
                    : `继续显示 ${commentItem._visibleReplyCount}/${commentItem.replyCount}` }}
                </button>
                <button class="reply-collapse" type="button" @click="collapseReplies(commentItem)">
                  收起回复
                </button>
              </template>
            </div>

            <form
              v-if="replyTarget && String(replyTarget.id) === String(commentItem.id)"
              class="reply-box"
              @submit.prevent="submitReply(commentItem)"
            >
              <div v-if="mentionOpen && mentionTarget === 'reply'" class="mention-panel reply-mention-panel">
                <div v-if="mentionLoading" class="mention-empty">加载中…</div>
                <div v-else-if="!mentionCandidates.length" class="mention-empty">没有匹配的关注用户</div>
                <button
                  v-for="user in mentionCandidates"
                  :key="user.id"
                  type="button"
                  class="mention-item"
                  @mousedown.prevent="selectMention(user)"
                >
                  <span>{{ avatarText(user.avatar, user.nickname) }}</span>
                  <b>{{ user.nickname || '灵感用户' }}</b>
                  <small>已关注</small>
                </button>
              </div>
              <input
                v-model="replyText"
                :placeholder="replyToUser ? `回复 @${replyToUser.nickname}` : '回复...'"
                :maxlength="commentMax"
                @input="onMentionInput($event, 'reply')"
                @keydown.esc="closeMention"
              >
              <button type="submit" :disabled="!replyText.trim() || submittingComment">发送</button>
              <button type="button" class="reply-cancel" @click="cancelReply">取消</button>
            </form>
          </div>
        </div>
      </div>
      </AppState>

      <div v-if="isLogin && commentState === 'ready'" class="comment-load-status">
        <span v-if="commentLoading">正在加载评论...</span>
        <button
          v-else-if="commentHasMore"
          class="comment-load-btn"
          type="button"
          @click="loadMoreComments"
        >
          点击加载下一批 · 当前显示 {{ loadedCommentCount }}/{{ commentTotal }}
        </button>
        <span v-else>已显示全部 {{ commentTotal }} 条评论</span>
      </div>

<div class="bottom-action-bar">
        <div v-if="mentionOpen && mentionTarget === 'quick'" class="mention-panel quick-mention-panel">
          <div v-if="mentionLoading" class="mention-empty">加载中…</div>
          <div v-else-if="!mentionCandidates.length" class="mention-empty">没有匹配的关注用户</div>
          <button
            v-for="user in mentionCandidates"
            :key="user.id"
            type="button"
            class="mention-item"
            @mousedown.prevent="selectMention(user)"
          >
            <span>{{ avatarText(user.avatar, user.nickname) }}</span>
            <b>{{ user.nickname || '灵感用户' }}</b>
            <small>已关注</small>
          </button>
        </div>
        <form class="quick-comment" @submit.prevent="submitComment">
          <input
            v-model="quickCommentText"
            :readonly="!isLogin"
            placeholder="说点什么，回车发送"
            :maxlength="commentMax"
            enterkeyhint="send"
            aria-label="快速评论"
            @focus="handleQuickCommentFocus"
            @input="onMentionInput($event, 'quick')"
            @keydown.esc="closeMention"
          >
        </form>
        <div class="mini-action-group">
          <button class="mini-action" type="button" title="引用再创作" @click="emit('quote')">
            <span>❝</span><small>引用</small>
          </button>
          <button class="mini-action" :class="{ active: liked }" type="button" title="点赞" @click="emit('like')">
            <span>{{ liked ? '♥' : '♡' }}</span><small>{{ detail.likeCount ?? 0 }}</small>
          </button>
          <button class="mini-action" :class="{ active: collected }" type="button" title="收藏" @click="emit('collect')">
            <span>{{ collected ? '★' : '☆' }}</span><small>{{ detail.collectCount ?? 0 }}</small>
          </button>
        </div>
      </div>
</template>

<script setup>
import {toRef, watch} from 'vue'
import {useInspireComments} from '../composables/useInspireComments.js'
import {formatCommentTime} from '@/utils/time.js'
import {INPUT_LIMITS} from '@/utils/validation.js'

const props = defineProps({
  detail: { type: Object, required: true },
  isLogin: { type: Boolean, default: false },
  liked: { type: Boolean, default: false },
  collected: { type: Boolean, default: false }
})

const emit = defineEmits(['require-login', 'quote', 'like', 'collect'])
const commentMax = INPUT_LIMITS.commentMax

const {
  commentTotal,
  commentSort,
  commentLoading,
  quickCommentText,
  replyText,
  replyTarget,
  replyToUser,
  submittingComment,
  mentionOpen,
  mentionLoading,
  mentionTarget,
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
  displayComments,
  commentState,
  loadedCommentCount,
  commentHasMore,
  resetComments
} = useInspireComments({
  detail: toRef(props, 'detail'),
  isLogin: toRef(props, 'isLogin'),
  requireLogin: () => emit('require-login')
})

const isImageAvatar = (avatar) => typeof avatar === 'string'
  && (avatar.startsWith('http') || avatar.startsWith('/') || avatar.startsWith('data:'))
const firstGrapheme = (value) => {
  const chars = Array.from(String(value || '').trim())
  return chars.length ? chars[0] : ''
}
const avatarText = (avatar, name) => {
  if (avatar && !isImageAvatar(avatar)) return firstGrapheme(avatar) || firstGrapheme(name) || '灵'
  return firstGrapheme(name) || '灵'
}

watch(() => props.detail?.id, (id) => {
  resetComments()
  if (props.isLogin && id) loadComments(true, id)
}, {immediate: true})
</script>

<style scoped>
.comments-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 28px;
}

.comments-title { margin: 0; font-size: 15px; font-weight: 700; }

.comment-sort-toggle {
  height: auto;
  padding: 0;
  border: 0;
  background: transparent;
  color: #82908b;
  font-size: 10px;
  font-weight: 400;
  line-height: 1.4;
  cursor: pointer;
}

.comment-sort-toggle:disabled { opacity: .55; cursor: default; }

/* 游客状态：评论区占位块，点击跳登录 */
.comment-gate {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 100%;
  margin-top: 28px;
  padding: 26px 18px;
  border: 2px dashed rgba(79, 138, 72, .5);
  border-radius: 18px;
  background: #e6f2e4;
  color: #3f6b3a;
  font-family: inherit;
  text-align: center;
  cursor: pointer;
}
.comment-gate:active { background: #dcecd9; }
.comment-gate-title { font-size: 15px; font-weight: 700; }
.comment-gate-desc { font-size: 12.5px; color: #6f8f6a; }
.comment-gate-btn {
  margin-top: 6px;
  padding: 7px 20px;
  border-radius: 999px;
  background: #4f8a48;
  color: #f3faf2;
  font-size: 13px;
  font-weight: 700;
}

.comment-list { margin-top: 5px; }

.comment-root {
  margin-top: 15px;
  padding: 15px 13px 6px;
  border-top: 1px solid rgba(230, 120, 51, .24);
  border-radius: 17px;
  background: rgba(255, 249, 240, .58);
  text-align: left;
  /* 长列表优化：评论是变高元素，用 content-visibility 让浏览器跳过屏外条目的
     渲染与布局（等价于浏览器内置的懒渲染），200+ 条评论滚动也不会卡。
     contain-intrinsic-size 提供占位高度，避免滚动条跳动。 */
  content-visibility: auto;
  contain-intrinsic-size: auto 130px;
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 9px;
  margin-bottom: 8px;
  text-align: left;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin-left: 0;
  border-radius: 50%;
  color: #4f8a48;
  background: #e6f2e4;
  font-size: 14px;
  font-weight: 700;
}

.comment-head b { font-size: 13.5px; text-align: left; }

.comment-head > span:last-child {
  margin-left: auto;
  color: #9a7653;
  font-size: 11.5px;
  opacity: .88;
}

.comment-text {
  margin: 0;
  color: #624734;
  font-size: 13.8px;
  line-height: 1.72;
  word-break: break-word;
}

.comment-tools {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  color: #9a7653;
  font-size: 12px;
}

.comment-tools span { cursor: pointer; }
.comment-like.liked { color: #e65f20; font-weight: 700; }

.replies {
  margin: 11px 0 0 22px;
  padding: 10px 12px;
  border-left: 3px solid #e67833;
  border-radius: 15px;
  background: rgba(255, 232, 203, .45);
}

.replies.empty {
  margin: 0;
  padding: 0;
  border: 0;
  background: none;
}

.reply { padding: 10px 0; border-top: 1px dotted rgba(103, 64, 38, .46); }
.reply:first-child { padding-top: 0; border-top: 0; }
.reply-to {
  display: inline;
  padding: 0;
  border-radius: 0;
  color: #4f7d73;
  background: transparent;
  font-weight: 600;
}

.reply-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 9px;
  font-size: 12px;
}

.reply-toggle,
.reply-more { color: #b75b22; font-weight: 700; cursor: pointer; }
.reply-collapse { color: #9a7653; cursor: pointer; }

.reply-actions button {
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
}

.reply-box { position: relative; display: flex; gap: 7px; margin-top: 11px; }

.reply-box input {
  flex: 1;
  min-width: 0;
  height: 38px;
  padding: 0 12px;
  border: 1.5px solid #e67833;
  border-radius: 11px;
  outline: none;
  color: #674026;
  background: rgba(255, 255, 255, .48);
  font-family: inherit;
  font-size: 13px;
}

.reply-box button {
  flex: 0 0 auto;
  padding: 0 13px;
  border: 0;
  border-radius: 11px;
  color: #fff7ed;
  background: #e67833;
  font-size: 12.5px;
  cursor: pointer;
}

.reply-box button:disabled { opacity: .4; cursor: default; }

.reply-box .reply-cancel {
  color: #9a7653;
  background: transparent;
  border: 1px dashed rgba(103, 64, 38, .5);
}

.comment-load-status {
  margin: 18px 0 8px;
  color: #a86631;
  text-align: center;
  font-size: 12.5px;
  font-weight: 600;
}
.comment-load-btn {
  min-height: 38px;
  padding: 0 18px;
  border: 1px solid #e8bd94;
  border-radius: 999px;
  background: #fff8f0;
  color: #a85b25;
  font: inherit;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}
.comment-load-btn:active { transform: scale(.985); }

.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  z-index: 40;
  width: min(100%, 860px);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px 11px;
  transform: translateX(-50%);
  border-top: 1px dashed #d9905d;
  background: rgba(255, 239, 218, .97);
  backdrop-filter: blur(12px);
}

.quick-comment {
  flex: 1;
  min-width: 0;
  height: 42px;
  overflow: hidden;
  border: 2px solid #e67833;
  border-radius: 999px;
  background: #fff9f0;
}

.quick-comment input {
  width: 100%;
  height: 100%;
  padding: 0 16px;
  border: 0;
  outline: none;
  color: #674026;
  background: transparent;
  font-family: inherit;
  font-size: 13.5px;
}

.quick-comment input::placeholder { color: #b78a63; }
.mini-action-group { display: flex; align-items: center; gap: 6px; flex: 0 0 auto; }
.mini-action {
  width: 42px;
  height: 42px;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
  border: 1px solid rgba(214, 159, 112, .5);
  border-radius: 12px;
  background: rgba(255, 255, 255, .78);
  color: #795238;
  font: inherit;
  cursor: pointer;
}
.mini-action span { font-size: 17px; line-height: 1; }
.mini-action small { margin-top: 2px; font-size: 9px; line-height: 1; }
.mini-action.active { border-color:#e67833; background:#fff4e7; color:#c85b1d; }
.mention-panel {
  position: absolute;
  z-index: 80;
  left: 14px;
  right: 14px;
  bottom: calc(100% + 8px);
  max-height: 236px;
  overflow: auto;
  padding: 7px;
  border: 1px solid #dcebe8;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 38px rgba(34, 70, 62, .18);
}
.reply-mention-panel {
  left: 0;
  right: 0;
  bottom: calc(100% + 7px);
}
.mention-empty { padding: 22px 12px; text-align: center; color: #93a5a1; font-size: 12px; }
.mention-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: #294a45;
  text-align: left;
  cursor: pointer;
}
.mention-item:hover,
.mention-item:focus-visible { outline: 0; background: #f2faf8; }
.mention-item > span {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #e8f6f2;
  color: #0f766e;
  font-size: 13px;
}
.mention-item b { min-width: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12.5px; }
.mention-item small { color: #9aaba7; font-size: 10.5px; }

@media (max-width: 640px) {
  .replies { margin-left: 10px; padding: 9px 10px; }
  .bottom-action-bar { gap: 6px; padding: 8px 9px 10px; }
  .quick-comment { height: 42px; }
  .mini-action-group { gap: 5px; }
  .mini-action { width: 39px; height: 42px; }
}
</style>
