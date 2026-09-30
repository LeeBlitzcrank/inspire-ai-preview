/**
 * 文件：frontend/src/components/base/index.js
 * 所属模块：可复用 Vue 组件
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import AppCard from './AppCard.vue'
import AppEmpty from './AppEmpty.vue'
import AppSkeleton from './AppSkeleton.vue'
import AppState from './AppState.vue'
import AppTag from './AppTag.vue'
import AppErrorDialog from './AppErrorDialog.vue'
import CollectFolderDialog from './CollectFolderDialog.vue'

const components = { AppCard, AppEmpty, AppSkeleton, AppState, AppTag, AppErrorDialog, CollectFolderDialog }

export default {
  install(app) {
    Object.entries(components).forEach(([name, component]) => {
      app.component(name, component)
    })
  }
}

export { AppCard, AppEmpty, AppSkeleton, AppState, AppTag, AppErrorDialog, CollectFolderDialog }
