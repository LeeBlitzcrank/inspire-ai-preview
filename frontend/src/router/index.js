import { createRouter, createWebHistory } from 'vue-router'
import Index from '@/pages/index.vue'
const Search = () => import('@/pages/Search.vue')
const InspireDetail = () => import('@/pages/InspireDetail.vue')
const Personal = () => import('@/pages/Personal.vue')
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
const NotFound = () => import('@/pages/NotFound.vue')
const Notifications = () => import("@/pages/Notifications.vue")
const Forbidden = () => import('@/pages/Forbidden.vue')
	const ServerError = () => import('@/pages/ServerError.vue')
	const Collections = () => import('@/pages/Collections.vue')
	const Messages = () => import('@/pages/Messages.vue')

const routes = [
  { path: '/', name: 'Index', component: Index },
  { path: '/search', name: 'Search', component: Search },
  { path: '/detail/:id', name: 'InspireDetail', component: InspireDetail, meta: { needLogin: true } },
  { path: '/personal', name: 'Personal', component: Personal, meta: { needLogin: true } },
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
      { path: 'config', component: AdminConfig },
    ]
  },
  { path: "/403", name: "Forbidden", component: Forbidden },
  { path: "/500", name: "ServerError", component: ServerError },
  { path: "/:pathMatch(.*)*", name: "NotFound", component: NotFound }
]
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0, behavior: 'smooth' } }
})
router.beforeEach((to, from, next) => {
  const login = localStorage.getItem('isLogin')
  const adminToken = localStorage.getItem('adminToken')
  if (to.meta.needLogin && !login) next('/login')
  else if (to.meta.needAdmin && !adminToken) next('/admin/login')
  else next()
})
export default router
