import { createRouter, createWebHistory } from 'vue-router'
const Index = () => import('@/pages/Index.vue')
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

const routes = [
  { path: '/', name: 'Index', component: Index },
  { path: '/search', name: 'Search', component: Search },
  { path: '/detail/:id', name: 'InspireDetail', component: InspireDetail },
  { path: '/personal', name: 'Personal', component: Personal, meta: { needLogin: true } },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/create', name: 'Create', component: Create, meta: { needLogin: true } },
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
  }
]
const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to, from, next) => {
  const login = localStorage.getItem('isLogin')
  const adminToken = localStorage.getItem('adminToken')
  if (to.meta.needLogin && !login) next('/login')
  else if (to.meta.needAdmin && !adminToken) next('/admin/login')
  else next()
})
export default router
