import $ from 'jquery'

export default {
    state: {
        id: "",
        username: "",
        photo: "",
        token: "",
        is_login: false,
        pulling_info: true, //是否正在从云端拉取信息
    },
    getters: {
    },
    mutations: {  //要用mutation中的函数则要用commit
        updateUser(state, user) {
            state.id = user.id;
            state.username = user.username;
            state.photo = user.photo;
            state.is_login = user.is_login;
        },
        updateToken(state, token) {
            state.token = token;
        },
        logout(state) {
            state.id = "";
            state.username = "";
            state.photo = "";
            state.token = "";
            state.is_login = false;
        },
        updatePullingInfo(state, pulling_info) {
            state.pulling_info = pulling_info
        }
    },
    actions: {
        // actions 中定义的是异步操作，比如从云端请求数据。
        // 调用 actions 中的方法时需要使用 dispatch。
        // 异步操作完成后通常会调用 mutations 中的方法来修改 state。
        // 相比之下，mutations 中的方法是同步的，直接修改 state，使用 commit 调用
        // “从云端拉取信息”通常指的就是“从后端服务器获取数据”。

        // 在前端项目中，比如 Vue + Vuex 的结构里：

        // 前端是用户在浏览器中看到的界面。

        // 后端是存储数据和业务逻辑的服务器，可能是一个使用 Node.js、Spring Boot、Python Django 等构建的 API 服务。

        // 所谓“云端”就是指部署在远程服务器上的后端服务，也可以简单理解为“从服务器获取数据”。
        login(context, data) {
            $.ajax({
                url: "http://127.0.0.1:3000/user/account/token/",
                type: "post",
                data: {
                    username: data.username,
                    password: data.password,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        sessionStorage.setItem("jwt_token", resp.token);
                        console.log(resp.token);
                        context.commit("updateToken", resp.token);//在action中调用mutation中的函数要用commit,并且将函数用""进行引用
                        data.success(resp);
                    } else {
                        data.error(resp);
                    }
                },
                error(resp) {
                    console.error("API 请求失败:", resp)
                    data.error(resp);
                }
            });
        },
        getinfo(context, data) {
            console.log("123456789999999");
            $.ajax({
                url: "http://127.0.0.1:3000/user/account/info/",
                type: "get",
                headers: {
                    Authorization: "Bearer " + context.state.token,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        context.commit("updateUser", {
                            ...resp, //将resp的内容解析出来
                            is_login: true,
                        });
                        data.success(resp);
                    } else {
                        data.error(resp);
                    }
                },
                error(resp) {
                    console.error("获取信息的API 请求失败:", resp);
                    data.error(resp);
                }
            });
        },
        logout(context) {
            sessionStorage.removeItem("jwt_token");
            context.commit("logout");
        }
    },
    modules: {
    }
}