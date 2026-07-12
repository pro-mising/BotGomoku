<template>
  <div ref="parent" class="gamemap">
    <canvas ref="canvas" tabindex="0"></canvas>
  </div>
</template>

<script>
import { GameMap } from '@/assets/scripts/GameMap';
import { ref, onMounted, onUnmounted } from 'vue'
import { useStore } from 'vuex';

export default {
  setup() {
    const store = useStore();
    let parent = ref(null);
    let canvas = ref(null);

    onMounted(() => {
      if (store.state.pk.gameObject) {
        store.state.pk.gameObject.destroy();
      }
      store.commit("updateGameObject",
        new GameMap(canvas.value.getContext('2d'), parent.value, store)
      );
    });

    onUnmounted(() => {
      if (store.state.pk.gameObject) {
        store.state.pk.gameObject.destroy();
        store.commit("updateGameObject", null);
      }
    });

    return {
      parent,
      canvas,
    }
  }
}
</script>

<style scoped>
.gamemap {
    width: 100%;
    aspect-ratio: 1 / 1;
    min-height: 420px;
    display: flex;
    justify-content: center;
    align-items: center;
}

canvas {
    width: 100%;
    height: 100%;
    display: block;
    border-radius: 6px;
}
</style>
