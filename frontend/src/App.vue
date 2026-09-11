<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="content">
    <router-view v-slot="{ Component }">
      <!-- 缓存需要缓存的页面 -->
      <keep-alive>
        <component :is="Component" v-if="$route.meta.keepAlive" :key="$route.name" />
      </keep-alive>
      
      <!-- 不缓存的页面正常渲染 -->
      <component :is="Component" v-if="!$route.meta.keepAlive" :key="$route.name" />
    </router-view>
  </div>

</template>

<style scoped>
.content{
  width: 100%;
  height: 100%;
  overflow: hidden;
  margin: 0px;
}
</style>
