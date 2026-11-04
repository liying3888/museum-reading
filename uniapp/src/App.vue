<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAppStore } from './stores/app'
import { useUserStore } from './stores/user'
const appStore = useAppStore()
const { getUser } = useUserStore()

onLaunch(async () => {
    await appStore.getConfig()
    // #ifdef H5
    const { status, close, url } = appStore.getH5Config
    if (status == 0) {
        if (close == 1) return (location.href = url)
        uni.reLaunch({ url: '/pages/empty/empty' })
    }
    // #endif
    // token 失效属正常态（拦截器已 logout 清缓存），勿让 onLaunch 挂 unhandled reject
    await getUser().catch(() => {})
})
</script>
<style lang="scss">
//
</style>
