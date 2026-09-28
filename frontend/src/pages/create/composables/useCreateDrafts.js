import {onUnmounted, ref} from 'vue'

export function useCreateDrafts({form, editId, coverImage, quoteSource, clampTitle, setContent}) {
  const LOCAL_DRAFT_KEY = 'inspire:local-drafts:v1'
  const localDrafts = ref([])
  const localDraftId = ref('')
  const draftPanelOpen = ref(false)
  let draftSaveTimer = null
  const draftHydrating = ref(true)

  const loadLocalDrafts = () => {
    try {
      localDrafts.value = JSON.parse(localStorage.getItem(LOCAL_DRAFT_KEY) || '[]')
    } catch {
      localDrafts.value = []
    }
  }

  const persistLocalDrafts = () => {
    localStorage.setItem(LOCAL_DRAFT_KEY, JSON.stringify(localDrafts.value.slice(0, 30)))
  }

  const draftHasContent = () =>
    Boolean(form.value.title.trim() || form.value.content.trim() || form.value.images.length || form.value.tag)

  const saveLocalDraftNow = () => {
    if (editId.value || draftHydrating.value || !draftHasContent()) return
    if (!localDraftId.value) localDraftId.value = `local-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
    const item = {
      id: localDraftId.value,
      title: form.value.title,
      tag: form.value.tag,
      content: form.value.content,
      images: form.value.images.filter(url => !String(url).startsWith('blob:')),
      img: coverImage.value,
      quoteInspireId: quoteSource.value?.id || '',
      publishCity: form.value.publishCity,
      updatedAt: Date.now()
    }
    localDrafts.value = [item, ...localDrafts.value.filter(d => d.id !== item.id)]
    persistLocalDrafts()
  }

  const scheduleLocalDraft = () => {
    clearTimeout(draftSaveTimer)
    draftSaveTimer = setTimeout(saveLocalDraftNow, 800)
  }

  const openLocalDraft = (item) => {
    draftHydrating.value = true
    localDraftId.value = item.id
    form.value = {
      title: clampTitle(item.title || ''),
      tag: item.tag || '',
      content: item.content || '',
      images: Array.isArray(item.images) ? item.images : [],
      img: item.img || '',
      publishCity: item.publishCity || ''
    }
    coverImage.value = item.img || form.value.images[0] || ''
    quoteSource.value = null
    setContent(form.value.content)
    draftPanelOpen.value = false
    draftHydrating.value = false
  }

  const newLocalDraft = () => {
    form.value = { title: '', tag: '', content: '', images: [], img: '', publishCity: '' }
    coverImage.value = ''
    quoteSource.value = null
    localDraftId.value = ''
    setContent('')
    draftPanelOpen.value = false
  }

  const removeLocalDraft = (id) => {
    localDrafts.value = localDrafts.value.filter(item => item.id !== id)
    if (localDraftId.value === id) localDraftId.value = ''
    persistLocalDrafts()
  }

  const formatDraftTime = (ts) => {
    const d = new Date(ts || 0)
    return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }

  onUnmounted(() => clearTimeout(draftSaveTimer))

  return {
    localDrafts,
    localDraftId,
    draftPanelOpen,
    draftHydrating,
    loadLocalDrafts,
    saveLocalDraftNow,
    scheduleLocalDraft,
    openLocalDraft,
    newLocalDraft,
    removeLocalDraft,
    formatDraftTime
  }
}
