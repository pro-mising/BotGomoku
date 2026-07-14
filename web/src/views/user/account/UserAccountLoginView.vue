<template>
    <ContentField v-if="!$store.state.user.pulling_info">
        <div class="row justify-content-md-center">
            <div class="col-3">
                <form @submit.prevent="login">
                    <div class="mb-3">
                        <label for="username" class="form-label">用户名</label>
                        <input v-model="username" type="text" class="form-control" id="username" placeholder="请输入用户名">
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">密码</label>
                        <input v-model="password" type="password" class="form-control" id="password" placeholder="请输入密码">
                    </div>
                    <div class="error_message">{{ error_message }}</div>
                    <button type="submit" class="btn btn-primary">登录</button>
                </form>
            </div>
        </div>
    </ContentField>
</template>

<script>
import ContentField from "@/components/ContentField.vue";
import { useStore } from "vuex";
import { ref } from "vue";
import router from "@/router/index";

export default {
    components: {
        ContentField,
    },
    setup() {
        const store = useStore();
        const username = ref("");
        const password = ref("");
        const error_message = ref("");

        store.dispatch("logout");
        store.commit("updatePullingInfo", false);

        const describe_error = resp => {
            if (!resp) return "登录失败，请检查后端服务";
            if (resp.error_message === "request failed") {
                return `登录接口请求失败，HTTP状态码：${resp.status || "未知"}`;
            }
            return resp.error_message || "用户名或密码错误";
        };

        const login = () => {
            error_message.value = "";
            store.dispatch("login", {
                username: username.value,
                password: password.value,
                success() {
                    store.dispatch("getinfo", {
                        success() {
                            router.push({ name: "home" });
                        },
                        error(resp) {
                            if (resp && resp.error_message === "request failed") {
                                error_message.value = `登录状态获取失败，HTTP状态码：${resp.status || "未知"}`;
                            } else {
                                error_message.value = "登录状态获取失败，请重新登录";
                            }
                        },
                    });
                },
                error(resp) {
                    error_message.value = describe_error(resp);
                },
            });
        };

        return {
            username,
            password,
            error_message,
            login,
        };
    },
};
</script>

<style scoped>
button {
    width: 100%;
}

div.error_message {
    color: red;
}
</style>
