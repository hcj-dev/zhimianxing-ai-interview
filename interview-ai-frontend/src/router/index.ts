import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@/views/Layout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '仪表盘', icon: 'Odometer' },
        },
        {
          path: 'resumes',
          name: 'ResumeList',
          component: () => import('@/views/ResumeList.vue'),
          meta: { title: '简历管理', icon: 'Document' },
        },
        {
          path: 'resumes/upload',
          name: 'ResumeUpload',
          component: () => import('@/views/ResumeUpload.vue'),
          meta: { title: '上传简历', icon: 'Upload' },
        },
        {
          path: 'resumes/:id',
          name: 'ResumeDetail',
          component: () => import('@/views/ResumeDetail.vue'),
          meta: { title: '简历详情', icon: 'View' },
        },
        {
          path: 'resumes/:id/match-jd',
          name: 'JDMatch',
          component: () => import('@/views/JDMatch.vue'),
          meta: { title: 'JD匹配分析', icon: 'Connection' },
        },
        {
          path: 'interview/start',
          name: 'InterviewStart',
          component: () => import('@/views/InterviewStart.vue'),
          meta: { title: '开始面试', icon: 'VideoCamera' },
        },
        {
          path: 'interview/:sessionId',
          name: 'InterviewRoom',
          component: () => import('@/views/InterviewRoom.vue'),
          meta: { title: '面试进行中', icon: 'ChatDotRound' },
        },
        {
          path: 'interview/:sessionId/report',
          name: 'InterviewReport',
          component: () => import('@/views/InterviewReport.vue'),
          meta: { title: '面试报告', icon: 'DataAnalysis' },
        },
        {
          path: 'questions',
          name: 'QuestionBank',
          component: () => import('@/views/QuestionBank.vue'),
          meta: { title: '题库浏览', icon: 'Collection' },
        },
        {
          path: 'questions/wrong/:id',
          name: 'WrongQuestionDetail',
          component: () => import('@/views/WrongQuestionDetail.vue'),
          meta: { title: '错题详情', icon: 'Warning' },
        },
        {
          path: 'questions/wrong',
          name: 'WrongQuestions',
          component: () => import('@/views/WrongQuestions.vue'),
          meta: { title: '错题本', icon: 'Warning' },
        },
        {
          path: 'questions/:id',
          name: 'QuestionDetail',
          component: () => import('@/views/QuestionDetail.vue'),
          meta: { title: '题目详情', icon: 'Reading' },
        },
        {
          path: 'leaderboard',
          name: 'Leaderboard',
          component: () => import('@/views/Leaderboard.vue'),
          meta: { title: '排行榜', icon: 'Trophy' },
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/Profile.vue'),
          meta: { title: '个人设置', icon: 'User' },
        },
        // Admin routes
        {
          path: 'admin/stats',
          name: 'AdminStats',
          component: () => import('@/views/admin/AdminStats.vue'),
          meta: { title: '数据概览', icon: 'DataAnalysis' },
        },
        {
          path: 'admin/users',
          name: 'AdminUsers',
          component: () => import('@/views/admin/AdminUsers.vue'),
          meta: { title: '用户管理', icon: 'UserFilled' },
        },
        {
          path: 'admin/questions',
          name: 'AdminQuestions',
          component: () => import('@/views/admin/AdminQuestions.vue'),
          meta: { title: '题库管理', icon: 'Edit' },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.guest && token) {
    next('/dashboard')
  } else if (!to.meta.guest && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
