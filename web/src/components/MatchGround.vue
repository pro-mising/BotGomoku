<template>
    <div class="matchground">
        <div class="row">
            <div class="col-5">
                <div class="user-photo">
                    <img :src="$store.state.user.photo" alt="">
                </div>
                <div class="user-username">
                    {{ $store.state.user.username }}
                </div>
            </div>
            <div class="col-2">
                <div class="user-select-bot">
                    <select v-model="select_bot" class="form-select" aria-label="选择出战方式">
                        <option value="-1" selected>亲自出马</option>
                        <option v-for="bot in bots" :key="bot.id" :value="bot.id">
                            {{ bot.title }}
                        </option>
                    </select>
                </div>
            </div>
            <div class="col-5">
                <div class="user-photo">
                    <img :src="$store.state.pk.opponent_photo" alt="">
                </div>
                <div class="user-username">
                    {{ $store.state.pk.opponent_username }}
                </div>
            </div>
            <div class="col-12" style="text-align:center; padding-top:6vh;">
                <button type="button" class="btn btn-warning btn-lg" style="font-size: 28px; padding: 16px 32px;" @click="click_match_btn">{{ match_btn_info }}</button>
            </div>
        </div>
    </div>
</template>

<script>
import { ref } from 'vue';
import { useStore } from 'vuex';
import $ from 'jquery';

export default {
    setup() {
        const store = useStore();
        let match_btn_info = ref("开始匹配");
        let bots = ref([]);
        let select_bot = ref("-1");
        
        const click_match_btn = () => { //写一个事件,把这个时间绑定到按钮上面
            if(match_btn_info.value === "开始匹配") {
                match_btn_info.value = "取消";
                store.state.pk.socket.send(JSON.stringify({
                    event: "start-matching",
                    bot_id: select_bot.value,
                }));
            } else {
                match_btn_info.value = "开始匹配";
                store.state.pk.socket.send(JSON.stringify({
                    event: "stop-matching",
                }));
            }
        }

        const refresh_bots = () => {
            $.ajax({
                url: "http://127.0.0.1:3000/user/bot/getlist/",
                type: "get",
                headers: {
                    Authorization: "Bearer " + store.state.user.token,
                },
                success(resp) {
                    bots.value = resp; //resp是一个列表
                    console.log(resp);
                },
                error(err) {
                    console.error("请求失败", err); // 关键：这里可以看到错误信息
                }
            })
        }

        refresh_bots(); //从云端动态获取bots

        return {
            match_btn_info,
            click_match_btn,
            refresh_bots,
            bots,
            select_bot,
        }
    }
}

</script>

<style scoped>
.matchground {
    width: 60vw;
    height: 70vh;
    margin: 40px auto;
    background-color: rgba(50, 50, 50,0.5);
}

div.user-photo {
    text-align: center;
    padding-top: 8vh;
}

div.user-photo > img {
    border-radius: 50%;
    width: 33vh;
}

div.user-username {
    text-align: center;
    font-size: 24px;
    font-weight: 600;
    color: white;
    padding-top:2vh;
}
div.user-select-bot {
    padding-top:20vh;
}
div.user-select-bot > select {
    margin: 0 auto;
}
</style>
