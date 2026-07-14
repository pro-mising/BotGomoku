<template>
  <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container">
      <router-link class="navbar-brand" :to="{name: 'home'}">BotGomoku</router-link>
      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarText">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarText">
        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <li class="nav-item">
            <router-link :class="route_name === 'pk_index' ? 'nav-link active' : 'nav-link'" :to="{name: 'pk_index'}">对战</router-link>
          </li>
          <li class="nav-item">
            <router-link :class="route_name === 'bot_evaluation' ? 'nav-link active' : 'nav-link'" :to="{name: 'bot_evaluation'}">Bot测试</router-link>
          </li>
          <li class="nav-item">
            <router-link :class="route_name === 'record_index' || route_name === 'record_content' ? 'nav-link active' : 'nav-link'" :to="{name: 'record_index'}">对局列表</router-link>
          </li>
          <li class="nav-item">
            <router-link :class="route_name === 'community_index' || route_name === 'community_post' ? 'nav-link active' : 'nav-link'" :to="{name: 'community_index'}">社区</router-link>
          </li>
          <li class="nav-item">
            <router-link :class="route_name === 'ranklist_index' ? 'nav-link active' : 'nav-link'" :to="{name: 'ranklist_index'}">排行榜</router-link>
          </li>
        </ul>

        <ul class="navbar-nav" v-if="$store.state.user.is_login">
          <li class="nav-item online-status-item">
            <span :class="['online-status', $store.state.user.online ? 'online' : 'offline']">
              <i></i>{{ $store.state.user.online ? "在线" : "离线" }}
            </span>
          </li>
          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle" href="#" id="navbarDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              {{ $store.state.user.username }}
            </a>
            <ul class="dropdown-menu">
              <li><router-link class="dropdown-item" :to="{name: 'user_bot_index'}">我的Bot</router-link></li>
              <li><hr class="dropdown-divider"></li>
              <li><a class="dropdown-item" href="#" @click="logout">退出登录</a></li>
            </ul>
          </li>
        </ul>

        <ul class="navbar-nav" v-else-if="!$store.state.user.pulling_info">
          <li class="nav-item">
            <router-link class="nav-link" :to="{name: 'user_account_login'}">登录</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" :to="{name: 'user_account_register'}">注册</router-link>
          </li>
        </ul>
      </div>
    </div>
  </nav>
</template>

<script>
import { useRoute } from 'vue-router'
import { computed, onMounted, onUnmounted } from 'vue';
import { useStore } from 'vuex';
import $ from 'jquery';

export default {
  setup () {
    const route = useRoute();
    const store = useStore();
    const route_name = computed(() => route.name);
    let online_timer = null;

    const refresh_online_status = () => {
      if (!store.state.user.is_login || !store.state.user.token) return;
      $.ajax({
        url: "http://127.0.0.1:3000/user/account/online/",
        type: "GET",
        headers: {
          Authorization: "Bearer " + store.state.user.token,
        },
        success(resp) {
          if (resp.error_message === "success") {
            store.commit("updateOnline", resp.online);
          }
        }
      });
    };

    onMounted(() => {
      refresh_online_status();
      online_timer = setInterval(refresh_online_status, 15000);
    });

    onUnmounted(() => {
      if (online_timer) clearInterval(online_timer);
    });

    const logout = () => {
      store.dispatch("logout");
    }

    return {
      route_name,
      logout,
    }
  }
}
</script>

<style scoped>
.online-status-item {
  display: flex;
  align-items: center;
  padding-right: 10px;
}

.online-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
  font-weight: 700;
}

.online-status i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
}

.online-status.online i {
  background: #35b879;
  box-shadow: 0 0 0 3px rgba(53, 184, 121, 0.18);
}

.online-status.offline i {
  background: #94a3b8;
}
</style>
