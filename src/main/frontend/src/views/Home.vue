<template>
  <div class="home">
    <h1>流程管理系统</h1>
    <el-button type="primary" @click="goToProcess">进入流程管理</el-button>
  </div>
</template>

<script>
export default {
  name: 'Home',
  methods: {
    goToProcess() {
      this.$router.push('/process')
    }
  }
}
</script>

<style scoped>
.home {
  text-align: center;
  padding-top: 100px;
}
</style>