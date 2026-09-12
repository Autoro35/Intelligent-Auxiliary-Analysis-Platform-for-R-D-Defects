<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <el-icon :size="34" color="#409eff"><Monitor /></el-icon>
        <h2 class="login-title">研发缺陷管理与智能辅助分析平台</h2>
        <p class="login-subtitle">轻量化 · 全生命周期 · AI 辅助</p>
      </div>

      <el-tabs v-model="activeTab" stretch>
        <!-- 登录 -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @keyup.enter="onLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" clearable>
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password>
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="onLogin">登 录</el-button>
          </el-form>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="4-20 位，登录账号" clearable />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="至少 6 位" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" show-password />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="选填，展示用名称" clearable />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="选填" clearable />
            </el-form-item>
            <el-alert
              type="info"
              :closable="false"
              class="mb-16"
              title="新注册账号默认为「访客」角色，仅可查看，需管理员调整角色后才能提单。"
            />
            <el-button type="primary" class="submit-btn" :loading="loading" @click="onRegister">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { register as registerApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)

const loginFormRef = ref()
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerFormRef = ref()
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: ''
})
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度需在 4-20 位之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function onLogin() {
  try {
    await loginFormRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.push(redirect ? decodeURIComponent(redirect) : '/dashboard')
  } catch (e) {
    // 错误提示已在 axios 拦截器中统一处理
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  try {
    await registerFormRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await registerApi({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname || undefined,
      email: registerForm.email || undefined
    })
    ElMessage.success('注册成功，请登录')
    loginForm.username = registerForm.username
    loginForm.password = ''
    activeTab.value = 'login'
  } catch (e) {
    // 错误提示已在 axios 拦截器中统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #1f3b73 0%, #2a5298 50%, #409eff 100%);
}

.login-card {
  width: 420px;
  padding: 8px 12px;
  border-radius: 8px;
}

.login-header {
  text-align: center;
  margin-bottom: 8px;
}

.login-title {
  margin: 8px 0 4px;
  font-size: 18px;
  color: #303133;
}

.login-subtitle {
  margin: 0 0 12px;
  font-size: 12px;
  color: #909399;
  letter-spacing: 1px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}
</style>
