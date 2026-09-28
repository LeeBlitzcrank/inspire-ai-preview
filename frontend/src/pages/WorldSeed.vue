<template>
  <div class="world-page">
    <header class="world-topbar">
      <button class="icon-button" type="button" aria-label="返回" @click="goBack">←</button>
      <div class="title-copy">
        <b>{{ pageTitle }}</b>
        <span>{{ pageSubtitle }}</span>
      </div>
      <button
        v-if="view === 'home'"
        class="icon-button create-button"
        type="button"
        aria-label="新建世界种子"
        @click="openCreate"
      >＋</button>
      <span v-else class="mode-badge">{{ view === 'detail' ? '公版共创' : 'AI 推演' }}</span>
    </header>

    <section v-if="view === 'home'" class="home-view">
      <article class="intro-panel">
        <small>从熟悉的故事出发</small>
        <h1>改变一个条件，看故事走向另一条世界线。</h1>
        <p>原文负责建立人物和情感前提，AI 只负责推演分支，并明确标注为平行创作。</p>
      </article>

      <div class="section-head">
        <b>正在运行的世界</b>
        <span>{{ seeds.length }} 个种子</span>
      </div>

      <AppState
        :state="listState"
        :rows="4"
        empty-icon="📖"
        empty-text="还没有世界种子"
        error-text="世界列表加载失败"
        @retry="loadSeeds"
      >
        <button
          v-for="(seed, index) in seeds"
          :key="seed.id"
          class="seed-card"
          type="button"
          @click="openDetail(seed)"
        >
          <div class="seed-cover" :class="`tone-${index % 4}`">
            <span>{{ seed.sourceAuthor || '公版作品' }}</span>
            <b>{{ seed.sourceTitle }}</b>
            <i>PARALLEL</i>
          </div>
          <div class="seed-copy">
            <h2>{{ seed.title }}</h2>
            <p>{{ seed.question || '世界仍在等待第一个关键选择。' }}</p>
            <div class="seed-meta">
              <span><b>{{ seed.branches }}</b> 条世界线</span>
              <span>进入故事 →</span>
            </div>
          </div>
        </button>
      </AppState>
    </section>

    <section v-else-if="view === 'detail'" class="detail-view">
      <AppState
        :state="detailState"
        :rows="6"
        empty-icon="📖"
        empty-text="世界不存在"
        error-text="世界详情加载失败"
        @retry="selectedSeed && loadDetail(selectedSeed.id)"
      >
        <template v-if="selectedSeed">
          <article class="source-panel">
            <small>{{ selectedSeed.sourceAuthor || '公版作品' }} · 原文锚点</small>
            <b>{{ selectedSeed.sourceTitle }}</b>
            <p>{{ selectedSeed.sourceText }}</p>
          </article>

          <article class="world-panel">
            <div class="panel-head">
              <div>
                <small>世界初始背景</small>
                <h1>{{ selectedSeed.title }}</h1>
              </div>
              <span>{{ textLength(selectedSeed.background) }} 字</span>
            </div>
            <div class="world-copy" :class="{ collapsed: !backgroundExpanded }">
              <p v-for="(paragraph, index) in paragraphs(selectedSeed.background)" :key="index">
                {{ paragraph }}
              </p>
            </div>
            <button class="text-button" type="button" @click="backgroundExpanded = !backgroundExpanded">
              {{ backgroundExpanded ? '收起背景' : '展开完整背景' }}
            </button>
            <div v-if="selectedSeed.rules?.length" class="rule-list">
              <span v-for="rule in selectedSeed.rules" :key="rule">{{ rule }}</span>
            </div>
          </article>

          <div class="section-head">
            <b>选择世界线</b>
            <span>{{ selectedSeed.lines?.length || 0 }} 条主题线</span>
          </div>
          <div class="line-tabs">
            <button
              v-for="line in selectedSeed.lines"
              :key="line.id"
              type="button"
              :class="{ active: activeLineId === line.id }"
              @click="selectLine(line.id)"
            >{{ line.title }}</button>
          </div>

          <div v-if="activeLine?.branches?.length" class="branch-tabs">
            <button
              v-for="branch in activeLine.branches"
              :key="branch.id"
              type="button"
              :class="{ active: activeBranchId === branch.id }"
              @click="selectBranch(branch.id)"
            >
              <b>{{ branch.defaultBranch ? '公共原线' : `${branch.ownerName} 的版本` }}</b>
              <span>{{ branch.chapterCount }} 章 · {{ branchVoteTotal(branch) }} 票</span>
            </button>
          </div>

          <article v-if="activeBranch" class="line-panel">
            <div class="line-heading">
              <span>当前分支</span>
              <b>{{ activeBranch.title }}</b>
            </div>
            <div class="fact-grid">
              <div><small>主题变量</small><b>{{ activeLine.variable || '暂未设定' }}</b></div>
              <div><small>章节数量</small><b>{{ activeBranch.chapterCount }} 章</b></div>
              <div><small>当前环境</small><b>{{ activeLine.environment || '暂未设定' }}</b></div>
              <div><small>未解问题</small><b>{{ activeLine.question || '继续观察故事变化' }}</b></div>
            </div>
            <div v-if="activeBranch.latestChapter" class="empty-chapter">
              <b>最新：第 {{ activeBranch.latestChapter.chapterNo }} 章 {{ activeBranch.latestChapter.title }}</b>
              <span>{{ activeBranch.latestChapter.excerpt }}</span>
            </div>

            <div v-if="chapters.length" class="chapter-list">
              <article v-for="chapter in chapters" :key="chapter.id" class="chapter-card">
                <div class="chapter-meta">
                  <span>第 {{ chapter.chapterNo }} 章</span>
                  <small>{{ textLength(chapter.content) }} 字</small>
                </div>
                <h3>{{ chapter.title }}</h3>
                <p v-for="(paragraph, index) in paragraphs(chapter.content)" :key="index">
                  {{ paragraph }}
                </p>
              </article>
              <button
                v-if="chapterHasMore"
                class="chapter-load-more"
                type="button"
                :disabled="chapterLoading"
                @click="loadChapters(false)"
              >{{ chapterLoading ? '加载中…' : '加载更早章节' }}</button>
            </div>
            <div v-else class="empty-chapter">
              <b>这一分支还没有生成章节</b>
              <span>选择一个方向，系统会在后台续写 800 到 1000 字。</span>
            </div>

            <div class="choice-panel">
              <div class="panel-head">
                <div><small>下一章抉择</small><b>你会把故事推向哪里？</b></div>
                <span>{{ voteTotal }} 人参与</span>
              </div>
              <div class="choice-list">
                <button
                  v-for="choice in choiceOptions"
                  :key="choice.key"
                  type="button"
                  :class="{ active: selectedChoiceKey === choice.key }"
                  @click="selectChoice(choice)"
                >
                  <span>{{ choice.label }}</span>
                  <small>{{ votePercent(choice.key) }}% 的读者选择</small>
                </button>
              </div>
              <textarea
                v-model="customChoice"
                maxlength="160"
                placeholder="也可以写下一个具体条件，例如：瀑布进入枯水期"
                @input="selectedChoiceKey = ''"
              />
              <button
                class="primary-button"
                type="button"
                :disabled="taskActive || generating || !activeBranch"
                @click="continueStory"
              >
                {{ generateButtonText }}
              </button>
              <div v-if="task" class="task-panel">
                <div class="task-copy">
                  <span>{{ task.status === 'PENDING' ? '任务排队中' : '正在生成并校验内容' }}</span>
                  <b>{{ task.progress || 0 }}%</b>
                </div>
                <div class="progress-track">
                  <i class="progress-bar" :style="{ width: `${task.progress || 0}%` }"></i>
                </div>
              </div>
            </div>
          </article>
        </template>
      </AppState>
    </section>

    <section v-else class="create-view">
      <div class="create-heading">
        <small>新建世界种子</small>
        <h1>先给 AI 一个真实、完整的故事锚点。</h1>
        <p>支持公版古诗、短篇和神话。生成内容会明确标注为平行创作，不冒充原文。</p>
      </div>

      <label class="field">
        <span>作品名称</span>
        <input v-model.trim="form.sourceTitle" maxlength="120" placeholder="例如：西游记·第一回">
      </label>
      <label class="field">
        <span>作者</span>
        <input v-model.trim="form.sourceAuthor" maxlength="80" placeholder="例如：吴承恩，可不填">
      </label>
      <label class="field">
        <span>原文内容</span>
        <textarea
          v-model.trim="form.sourceText"
          maxlength="6000"
          placeholder="粘贴公版原文或你拥有使用权的短文本"
        />
        <small>{{ form.sourceText.length }}/6000</small>
      </label>
      <label class="field">
        <span>创作方向</span>
        <textarea
          v-model.trim="form.guidance"
          maxlength="500"
          placeholder="例如：重点保留人物关系，生成 5 条差异明显的世界线"
        />
      </label>

      <button
        class="primary-button create-submit"
        type="button"
        :disabled="taskActive || creating || !form.sourceTitle || !form.sourceText"
        @click="createSeed"
      >
        {{ createButtonText }}
      </button>
      <div v-if="task" class="task-panel">
        <div class="task-copy">
          <span>{{ task.status === 'PENDING' ? '任务排队中' : '正在生成并校验世界设定' }}</span>
          <b>{{ task.progress || 0 }}%</b>
        </div>
        <div class="progress-track">
          <i class="progress-bar" :style="{ width: `${task.progress || 0}%` }"></i>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import {onMounted} from 'vue'
import AppState from '@/components/base/AppState.vue'
import {useWorldSeed} from './world/composables/useWorldSeed.js'

const {
  view,
  seeds,
  listState,
  detailState,
  selectedSeed,
  activeLine,
  activeBranch,
  activeLineId,
  activeBranchId,
  backgroundExpanded,
  selectedChoiceKey,
  customChoice,
  chapters,
  chapterHasMore,
  chapterLoading,
  task,
  taskActive,
  creating,
  generating,
  form,
  choiceOptions,
  pageTitle,
  pageSubtitle,
  voteTotal,
  generateButtonText,
  createButtonText,
  paragraphs,
  textLength,
  votePercent,
  branchVoteTotal,
  loadSeeds,
  loadDetail,
  loadChapters,
  openDetail,
  selectLine,
  selectBranch,
  selectChoice,
  openCreate,
  createSeed,
  continueStory,
  goBack
} = useWorldSeed()

onMounted(loadSeeds)
</script>

<style src="./world/styles/world.css"></style>
