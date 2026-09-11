<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header><span>个人信息</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ orDash(user.username) }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ orDash(user.nickname) }}</el-descriptions-item>
        <el-descriptions-item label="系统角色">
          <el-tag :type="roleTagType" size="small">{{ user.roleDesc || user.role }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="user.status === 1 ? 'success' : 'danger'" size="small">
            {{ user.status === 1 ? '正常' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ orDash(user.email) }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ orDash(user.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header><span>角色权限说明</span></template>
      <el-table :data="roleTable" border stripe>
        <el-table-column prop="role" label="角色" width="140" />
        <el-table-column prop="desc" label="说明" min-width="240" />
        <el-table-column prop="permission" label="权限范围" min-width="320" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { orDash } from '@/utils/format'

const userStore = useUserStore()

const user = computed(() => userStore.userInfo || {})

const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', TESTER: 'warning', DEVELOPER: 'primary', GUEST: 'info' }
  return map[user.value.role] || 'info'
})

const roleTable = [
  { role: '管理员 ADMIN', desc: '系统最高权限', permission: '全部功能，含用户列表、AI 状态自检、向量索引重建' },
  { role: '测试 TESTER', desc: '提单与验证', permission: '项目/缺陷/知识库读写，可用全部 AI 能力' },
  { role: '开发 DEVELOPER', desc: '处理缺陷', permission: '项目/缺陷/知识库读写，可用全部 AI 能力' },
  { role: '访客 GUEST', desc: '只读账号（注册默认角色）', permission: '仅可查看项目、缺陷、知识库与统计，可用 RAG 推荐' }
]

onMounted(async () => {
  // 进入个人中心时刷新一次，保证角色变更后立即生效
  await userStore.fetchUser()
})
</script>
