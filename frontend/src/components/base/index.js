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
