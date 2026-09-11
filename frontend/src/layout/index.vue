<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '210px'" class="layout-aside">
      <Sidebar :collapsed="collapsed" />
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <Navbar v-model:collapsed="collapsed" />
      </el-header>

      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <keep-alive :include="[]">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'

const collapsed = ref(false)
</script>

<style scoped>
.layout {
  height: 100%;
}

.layout-aside {
  background-color: #001529;
  transition: width 0.25s;
  overflow-x: hidden;
}

.layout-header {
  height: 60px;
  padding: 0 16px 0 0;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.layout-main {
  padding: 0;
  background-color: #f0f2f5;
  overflow-y: auto;
}
</style>
