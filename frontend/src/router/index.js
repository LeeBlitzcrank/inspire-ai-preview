import { createRouter, createWebHistory } from 'vue-router'
const Index = () => import('@/pages/Index.vue')
const Search = () => import('@/pages/Search.vue')
const Personal = () => import('@/pages/Personal.vue')
const InspireDetail = () => import('@/pages/InspireDetail.vue')
const Login = () => import('@/pages/Login.vue')
const Register = () => import('@/pages/Register.vue')
const Create = () => import('@/pages/Create.vue')
const ForgotPassword = () => import('@/pages/ForgotPassword.vue')
const ResetPasswordPage = () => import('@/pages/ResetPassword.vue')

const routes = [
  { path: '/', name: 'Index', component: Index },
  { path: '/search', name: 'Search', component: Search },
  { path: '/detail/:id', name: 'InspireDetail', component: InspireDetail },
  { path: '/personal', name: 'Personal', component: Personal, meta: { needLogin: true } },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/forgot-password', name: 'ForgotPassword', component: ForgotPassword },
  { path: '/reset-password', name: 'ResetPassword', component: ResetPasswordPage },
  { path: '/create', name: 'Create', component: Create, meta: { needLogin: true } }
]
const router = createRouter({
  history: createWebHistory(),
  routes
})
router.beforeEach((to, from, next) => {
  const login = localStorage.getItem('isLogin')
  if (to.meta.needLogin && !login) next('/login')
  else next()
})
export default router