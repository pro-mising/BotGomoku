<template>
    <ContentField>
        <div class="community-page">
            <header class="community-header">
                <div>
                    <div class="header-kicker">BotGomoku 社区</div>
                    <h2>玩家交流</h2>
                </div>
                <button class="btn btn-primary" @click="open_composer">发表帖子</button>
            </header>

            <section class="post-list" v-if="posts.length">
                <article class="post-card" v-for="post in posts" :key="post.id" @click="open_post(post.id)">
                    <div class="post-head">
                        <img :src="post.author.photo" alt="" class="avatar">
                        <div>
                            <div class="author-line">
                                <span class="author">{{ post.author.username }}</span>
                                <span :class="['presence-badge', post.author.online ? 'is-online' : 'is-offline']">
                                    <i></i>{{ post.author.online ? "在线" : "离线" }}
                                </span>
                            </div>
                            <div class="time">{{ post.createtime }}</div>
                        </div>
                        <span class="tag">{{ post.tag }}</span>
                    </div>
                    <h3>{{ post.title }}</h3>
                    <p>{{ excerpt(post.content) }}</p>
                    <div class="post-actions">
                        <button :class="post.liked ? 'btn btn-primary btn-sm' : 'btn btn-outline-primary btn-sm'" @click.stop="toggle_like(post)">
                            点赞 {{ post.likes }}
                        </button>
                        <button class="btn btn-outline-secondary btn-sm" @click.stop="open_post(post.id)">
                            评论 {{ post.comment_count }}
                        </button>
                        <button v-if="post.can_delete" class="btn btn-outline-danger btn-sm" @click.stop="remove_post(post)">
                            删除
                        </button>
                    </div>
                </article>
            </section>

            <section class="empty-state" v-else>
                暂时还没有帖子，成为第一个分享想法的玩家吧。
            </section>

            <nav v-if="pages.length">
                <ul class="pagination justify-content-end">
                    <li class="page-item" @click="click_page(-2)">
                        <a class="page-link" href="#">上一页</a>
                    </li>
                    <li :class="'page-item ' + page.is_active" v-for="page in pages" :key="page.number" @click="click_page(page.number)">
                        <a class="page-link" href="#">{{ page.number }}</a>
                    </li>
                    <li class="page-item" @click="click_page(-1)">
                        <a class="page-link" href="#">下一页</a>
                    </li>
                </ul>
            </nav>
        </div>

        <div class="composer-backdrop" v-if="show_composer" @click.self="close_composer">
            <section class="composer">
                <div class="composer-head">
                    <div>
                        <div class="header-kicker">新的讨论</div>
                        <h3>发表帖子</h3>
                    </div>
                    <button class="btn-close" type="button" aria-label="关闭" @click="close_composer"></button>
                </div>
                <input v-model="postadd.title" class="form-control" placeholder="标题">
                <div class="composer-row">
                    <select v-model="postadd.tag" class="form-select tag-select">
                        <option>Bot策略</option>
                        <option>五子棋技巧</option>
                        <option>代码问题</option>
                        <option>对局复盘</option>
                        <option>综合讨论</option>
                    </select>
                    <button class="btn btn-primary publish-btn" :disabled="is_publishing" @click="add_post">
                        {{ is_publishing ? "发布中..." : "发布" }}
                    </button>
                </div>
                <textarea v-model="postadd.content" class="form-control" rows="5" placeholder="分享你的 Bot 思路、对局复盘或五子棋技巧..."></textarea>
                <div class="error-message">{{ postadd.error_message }}</div>
            </section>
        </div>
    </ContentField>
</template>

<script>
import ContentField from "@/components/ContentField.vue";
import { reactive, ref } from "vue";
import { useStore } from "vuex";
import router from "@/router";
import $ from "jquery";

export default {
    components: {
        ContentField,
    },
    setup() {
        const store = useStore();
        const posts = ref([]);
        const pages = ref([]);
        const show_composer = ref(false);
        const is_publishing = ref(false);
        let current_page = 1;
        let total_posts = 0;

        const postadd = reactive({
            title: "",
            tag: "Bot策略",
            content: "",
            error_message: "",
        });

        const authHeaders = () => ({
            Authorization: "Bearer " + store.state.user.token,
        });

        const translate_error = message => {
            const messages = {
                "title cannot be empty": "标题不能为空",
                "title is too long": "标题太长",
                "content cannot be empty": "内容不能为空",
                "content is too long": "内容太长",
                "tag is too long": "分类太长",
                "post not found": "帖子不存在",
            };
            return messages[message] || message;
        };

        const excerpt = content => {
            if (!content) return "";
            return content.length > 180 ? content.substring(0, 180) + "..." : content;
        };

        const open_composer = () => {
            postadd.error_message = "";
            show_composer.value = true;
        };

        const close_composer = () => {
            show_composer.value = false;
        };

        const update_pages = () => {
            const max_pages = parseInt(Math.ceil(total_posts / 10));
            const new_pages = [];
            for (let i = current_page - 2; i <= current_page + 2; i++) {
                if (i >= 1 && i <= max_pages) {
                    new_pages.push({
                        number: i,
                        is_active: i === current_page ? "active" : "",
                    });
                }
            }
            pages.value = new_pages;
        };

        const pull_page = page => {
            current_page = page;
            $.ajax({
                url: "http://127.0.0.1:3000/community/post/list/",
                type: "GET",
                data: { page },
                headers: authHeaders(),
                success(resp) {
                    posts.value = resp.posts;
                    total_posts = resp.post_count;
                    update_pages();
                }
            });
        };

        const click_page = page => {
            if (page === -2) page = current_page - 1;
            else if (page === -1) page = current_page + 1;
            const max_pages = parseInt(Math.ceil(total_posts / 10));
            if (page >= 1 && page <= max_pages) pull_page(page);
        };

        const add_post = () => {
            postadd.error_message = "";
            if (!postadd.title.trim()) {
                postadd.error_message = "标题不能为空";
                return;
            }
            if (!postadd.content.trim()) {
                postadd.error_message = "内容不能为空";
                return;
            }
            is_publishing.value = true;
            $.ajax({
                url: "http://127.0.0.1:3000/community/post/add/",
                type: "POST",
                data: {
                    title: postadd.title.trim(),
                    tag: postadd.tag,
                    content: postadd.content.trim(),
                },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        postadd.title = "";
                        postadd.content = "";
                        close_composer();
                        pull_page(1);
                    } else {
                        postadd.error_message = translate_error(resp.error_message);
                    }
                },
                error() {
                    postadd.error_message = "发布失败，请检查后端服务是否正常运行";
                },
                complete() {
                    is_publishing.value = false;
                }
            });
        };

        const toggle_like = post => {
            $.ajax({
                url: post.liked
                    ? "http://127.0.0.1:3000/community/post/unlike/"
                    : "http://127.0.0.1:3000/community/post/like/",
                type: "POST",
                data: { post_id: post.id },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        post.liked = !post.liked;
                        post.likes += post.liked ? 1 : -1;
                        if (post.likes < 0) post.likes = 0;
                    }
                }
            });
        };

        const remove_post = post => {
            if (!confirm("确定要删除这篇帖子吗？")) return;
            $.ajax({
                url: "http://127.0.0.1:3000/community/post/remove/",
                type: "POST",
                data: { post_id: post.id },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        pull_page(current_page);
                    }
                }
            });
        };

        const open_post = postId => {
            router.push({
                name: "community_post",
                params: { postId },
            });
        };

        pull_page(current_page);

        return {
            posts,
            pages,
            postadd,
            show_composer,
            is_publishing,
            add_post,
            click_page,
            open_post,
            open_composer,
            close_composer,
            toggle_like,
            remove_post,
            excerpt,
        };
    }
}
</script>

<style scoped>
.community-page {
    display: grid;
    gap: 18px;
}

.community-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding-bottom: 6px;
    border-bottom: 1px solid #e5e7eb;
}

.header-kicker {
    color: #64748b;
    font-size: 13px;
    font-weight: 800;
}

h2,
h3 {
    margin: 0;
    color: #0f172a;
    font-weight: 900;
}

.post-list {
    display: grid;
    gap: 12px;
}

.post-card {
    padding: 18px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: white;
    cursor: pointer;
    transition: box-shadow 0.18s ease, transform 0.18s ease, border-color 0.18s ease;
}

.post-card:hover {
    transform: translateY(-1px);
    border-color: #cbd5e1;
    box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
}

.post-head {
    display: flex;
    align-items: center;
    gap: 10px;
}

.avatar {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    object-fit: cover;
}

.author {
    font-weight: 800;
}

.author-line {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.presence-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 800;
}

.presence-badge i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
}

.presence-badge.is-online {
    color: #047857;
    background: #d1fae5;
}

.presence-badge.is-online i {
    background: #10b981;
}

.presence-badge.is-offline {
    color: #64748b;
    background: #f1f5f9;
}

.presence-badge.is-offline i {
    background: #94a3b8;
}

.time {
    color: #64748b;
    font-size: 13px;
}

.tag {
    margin-left: auto;
    padding: 4px 10px;
    border-radius: 999px;
    background: #e0f2fe;
    color: #075985;
    font-weight: 700;
    font-size: 12px;
}

.post-card h3 {
    margin: 14px 0 8px;
    font-size: 20px;
}

p {
    color: #475569;
    white-space: pre-wrap;
}

.post-actions {
    display: flex;
    gap: 10px;
    margin-top: 12px;
}

.empty-state {
    padding: 42px 18px;
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
    background: #f8fafc;
    color: #64748b;
    text-align: center;
    font-weight: 700;
}

.composer-backdrop {
    position: fixed;
    inset: 0;
    z-index: 30;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: rgba(15, 23, 42, 0.42);
}

.composer {
    width: min(720px, 94vw);
    display: grid;
    gap: 12px;
    padding: 20px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 24px 70px rgba(15, 23, 42, 0.22);
}

.composer-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
}

.composer-row {
    display: flex;
    gap: 12px;
}

.tag-select {
    max-width: 220px;
}

.publish-btn {
    min-width: 120px;
}

.error-message {
    color: #dc3545;
    min-height: 20px;
}

@media (max-width: 640px) {
    .community-header,
    .composer-row {
        align-items: stretch;
        flex-direction: column;
    }

    .tag-select,
    .publish-btn {
        max-width: none;
        width: 100%;
    }
}
</style>
