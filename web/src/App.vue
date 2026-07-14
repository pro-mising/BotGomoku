<template>
  <NavBar>

  </NavBar>
  <router-view/>
</template>

<script>
import NavBar from '@/components/NavBar.vue'
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap/dist/js/bootstrap.min.js"
import { useRoute, useRouter } from "vue-router";
import { useStore } from "vuex";

export default {
  components: {
    NavBar
  },
setup() {
  const store = useStore();
  const router = useRouter();
  const route = useRoute();

  const finishPulling = () => {
    store.commit("updatePullingInfo", false);
    if (route.meta.requestAuth && !store.state.user.is_login) {
      router.push({ name: "user_account_login" });
    }
  };

  sessionStorage.removeItem("jwt_token");
  store.commit("logout");
  finishPulling();
}

}
</script>


<style>
body {
  background-image: url("@/assets/images/background7.png");
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}
</style>
