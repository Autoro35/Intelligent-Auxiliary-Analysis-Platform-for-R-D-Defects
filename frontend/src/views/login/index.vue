<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <el-icon :size="34" color="#409eff"><Monitor /></el-icon>
        <h2 class="login-title">研发缺陷管理与智能辅助分析平台</h2>
        <p class="login-subtitle">轻量化 · 全生命周期 · AI 辅助</p>
      </div>

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

      <!-- 不提供自助注册：新账号一律为访客角色且无法自助升级，统一由管理员在「用户管理」中创建并授权 -->
      <el-alert type="info" :closable="false" class="login-tip" title="账号由管理员统一分配，忘记密码请联系管理员重置。" />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loginFormRef = ref()
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
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
  margin-bottom: 12px;
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

.login-tip {
  margin-top: 16px;
}
</style>
