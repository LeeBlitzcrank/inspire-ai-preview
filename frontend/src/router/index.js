import { createRouter, createWebHashHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'

// 路由级进度条：切换页面时给一个即时反馈（配合路由懒加载）
NProgress.configure({ showSpinner: false, trickleSpeed: 120, minimum: 0.12 })
import Index from '@/pages/index.vue'
const Search = () => import('@/pages/Search.vue')
const InspireDetail = () => import('@/pages/InspireDetail.vue')
const Personal = () => import('@/pages/Personal.vue')
const Following = () => import('@/pages/Following.vue')
const Login = () => import('@/pages/Login.vue')
const Register = () => import('@/pages/Register.vue')
const Create = () => import('@/pages/Create.vue')
const ForgotPassword = () => import('@/pages/ForgotPassword.vue')
const ResetPasswordPage = () => import('@/pages/ResetPassword.vue')
const AdminLogin = () => import('@/pages/admin/AdminLogin.vue')
const AdminLayout = () => import('@/pages/admin/AdminLayout.vue')
const AdminDashboard = () => import('@/pages/admin/AdminDashboard.vue')
const AdminInspire = () => import('@/pages/admin/AdminInspire.vue')
const AdminUser = () => import('@/pages/admin/AdminUser.vue')
const AdminConfig = () => import('@/pages/admin/AdminConfig.vue')
const AdminCategory = () => import('@/pages/admin/AdminCategory.vue')
const AdminWordCloud = () => import('@/pages/admin/AdminWordCloud.vue')
const NotFound = () => import('@/pages/NotFound.vue')
const Notifications = () => import("@/pages/Notifications.vue")
const Forbidden = () => import('@/pages/Forbidden.vue')
	const ServerError = () => import('@/pages/ServerError.vue')
	const Collections = () => import('@/pages/Collections.vue')
	const Messages = () => import('@/pages/Messages.vue')

const routes = [
  { path: '/', name: 'Index', component: Index },
  { path: '/search', name: 'Search', component: Search },
  { path: '/detail/:id', name: 'InspireDetail', component: InspireDetail },
  { path: '/personal', name: 'Personal', component: Personal, meta: { needLogin: true } },
  { path: '/following', name: 'Following', component: Following, meta: { needLogin: true } },
  { path: '/notifications', name: 'Notifications', component: Notifications, meta: { needLogin: true } },
  { path: '/collections', name: 'Collections', component: Collections, meta: { needLogin: true } },
  { path: '/messages', name: 'Messages', component: Messages, meta: { needLogin: true } },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/create', name: 'Create', component: Create, meta: { needLogin: true } },
  { path: '/edit/:id', name: 'Edit', component: Create, meta: { needLogin: true } },
  { path: '/forgot-password', name: 'ForgotPassword', component: ForgotPassword },
  { path: '/reset-password', name: 'ResetPassword', component: ResetPasswordPage },
  { path: '/admin/login', name: 'AdminLogin', component: AdminLogin },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { needAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: AdminDashboard },
      { path: 'inspire', component: AdminInspire },
      { path: 'user', component: AdminUser },
      { path: 'category', component: AdminCategory },
      { path: 'word-cloud', component: AdminWordCloud },
      { path: 'config', component: AdminConfig },
    ]
  },
  { path: "/403", name: "Forbidden", component: Forbidden },
  { path: "/500", redirect: "/" },
  { path: "/:pathMatch(.*)*", name: "NotFound", component: NotFound }
]
const router = createRouter({
  // 方案A：hash 路由，URL 形如 /#/login，静态托管(GitHub Pages)无需 SPA 兜底，彻底消除 404
  history: createWebHashHistory(),
  routes,
  scrollBehavior() { return { top: 0, behavior: 'smooth' } }
})
router.beforeEach((to, from, next) => {
  NProgress.start()
  // 方案C：读取全局登录态 store（启动时已从 localStorage 同步，零延迟）
  const auth = useAuthStore(pinia)
  auth.init()
  const adminToken = localStorage.getItem('adminToken')
  if (to.meta.needLogin && !auth.isLogin) {
    next('/login')
  }
  else if (to.meta.needAdmin && !adminToken) next('/admin/login')
  else next()
})
router.afterEach(() => { NProgress.done() })
// 懒加载 chunk 拉取失败等异常也要收掉进度条，避免一直卡在加载态
router.onError(() => { NProgress.done() })

export default router
