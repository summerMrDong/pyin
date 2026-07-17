<template>
  <section class="login-page">
    <section class="login-panel">
      <div class="panel-head">
        <div class="brand-mark">P</div>
        <p class="eyebrow">Pyin Config Center</p>
      </div>

      <div class="panel-divider"></div>

      <section class="panel-body">
        <el-alert
          v-if="errorMessage"
          :closable="false"
          type="error"
          class="form-alert"
          :title="errorMessage"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="form-body"
          @keyup.enter="handleSubmit"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="请输入管理员账号"
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入登录密码"
              autocomplete="current-password"
            />
          </el-form-item>
          <el-button
            class="login-submit"
            type="primary"
            size="large"
            :loading="submitting"
            @click="handleSubmit"
          >
            登录
          </el-button>
        </el-form>
      </section>
    </section>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useShellNavigationStore } from '../stores/shellNavigation'

const authStore = useAuthStore()
const navigationStore = useShellNavigationStore()
const router = useRouter()
const route = useRoute()

const formRef = ref()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  username: 'admin',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleSubmit() {
  if (submitting.value) {
    return
  }

  try {
    const valid = await formRef.value?.validate()
    if (!valid) {
      return
    }

    submitting.value = true
    errorMessage.value = ''
    await authStore.login(form)
    await navigationStore.refresh(router)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = error?.payload?.message ?? error?.message ?? '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 20px;
  background:
    radial-gradient(circle at top, rgba(45, 212, 191, 0.08), transparent 26%),
    radial-gradient(circle at bottom right, rgba(14, 116, 144, 0.14), transparent 22%),
    linear-gradient(180deg, #071018 0%, #0c1622 100%);
}

.login-panel {
  width: min(520px, 100%);
  padding: 26px 26px 28px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  background:
    linear-gradient(180deg, rgba(11, 18, 32, 0.92), rgba(8, 13, 25, 0.98));
  box-shadow:
    0 26px 72px rgba(2, 6, 23, 0.36),
    inset 0 1px 0 rgba(255, 255, 255, 0.02);
}

.panel-head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
}

.brand-mark {
  width: 60px;
  height: 60px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  margin-bottom: 14px;
  border: 1px solid rgba(125, 211, 252, 0.18);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.86), rgba(8, 13, 25, 0.96));
  color: #e2e8f0;
  font-size: 22px;
  font-weight: 700;
  box-shadow: 0 18px 40px rgba(2, 6, 23, 0.3);
}

.eyebrow {
  margin: 12px 0 0;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: 11px;
  color: #67e8f9;
  font-weight: 600;
}

.panel-divider {
  width: 100%;
  height: 1px;
  margin: 20px 0 0;
  background: rgba(148, 163, 184, 0.12);
}

.panel-body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 320px;
  align-items: stretch;
}

.form-alert {
  margin-bottom: 18px;
}

.form-body {
  margin-top: 0;
  text-align: left;
}

.form-body :deep(.el-form-item) {
  display: grid;
  grid-template-columns: 56px 1fr;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
  width: 100%;
  padding: 8px 12px;
  background: rgba(2, 6, 23, 0.46);
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.12) inset;
  border-radius: 10px;
}

.form-body :deep(.el-form-item__label-wrap) {
  margin: 0;
  display: flex;
  align-items: center;
  height: 48px;
}

.form-body :deep(.el-form-item__label) {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  height: 48px;
  line-height: 48px;
  padding: 0;
}

.form-body :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  line-height: normal;
  min-height: 48px;
  width: 100%;
}

.form-body :deep(.el-input) {
  width: 100%;
}

.login-submit {
  align-self: stretch;
  width: 100%;
}

.login-submit {
  width: 100%;
  margin-top: 10px;
  height: 48px;
  border-radius: 10px;
}

:deep(.el-form-item__label) {
  color: #cbd5e1;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  background: transparent;
  box-shadow: none;
  min-height: 48px;
}

:deep(.el-input__inner) {
  color: #e5e7eb;
}

:deep(.el-input__inner::placeholder) {
  color: #64748b;
}

@media (max-width: 960px) {
  .login-panel {
    width: min(520px, 100%);
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 14px;
  }

  .login-panel {
    padding: 22px 18px 20px;
  }

  .panel-body {
    min-height: 300px;
  }
}
</style>
