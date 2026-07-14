<template>
  <NavBar>

  </NavBar>
  <router-view/>
</template>

<script>
import NavBar from '@/components/NavBar.vue'
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap/dist/js/bootstrap.min.js"
// import $ from 'jquery';

export default {
  components: {
    NavBar
  },
setup() {
  // $.ajax({
  //   url: "http://127.0.0.1:3000/user/account/token/",
  //   type: "post",
  //   data: {
  //     username: "nst",
  //     password: "250",
  //     confirmPassword: "250",
  //   },
  //   success(resp) {
  //     console.log(resp);
  //   },
  //   error(resp) {
  //     console.log(resp);
  //   }
  // }),

  // $.ajax({
  //   url:"http://127.0.0.1:3000/user/account/register/",
  //   type:"post",
  //   data: {
  //     username: "nst2",
  //     password: "250",
  //     confirmPassword: "250",
  //   },
  //   success(resp) {
  //     console.log(resp);
  //   },
  //   error(resp) {
  //     console.log(resp);
  //   }
  // })
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
