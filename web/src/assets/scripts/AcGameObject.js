const AC_GAME_OBJECTS = [];

export class AcGameObjects {
    constructor() {
        AC_GAME_OBJECTS.push(this);  //先创建的先push,先执行start,update,destroy，后执行的会把前执行的覆盖掉
        this.timedelta = 0;
        this.has_called_start = false;
    }

    start() { //只执行一次
    }

    update() { //除了第一帧之外，每一帧执行一次

    }

    on_destroy() { //删除之前执行

    }

    destroy() { //删除上一个对象重新生成新的
        this.on_destroy();

        for (let i in AC_GAME_OBJECTS) {
            const obj = AC_GAME_OBJECTS[i];
            if (obj === this) {
                AC_GAME_OBJECTS.splice(i);
                break;
            }
        }
    }

}

let last_timestamp; //上一次执行的时刻
const step = timestamp => {
    for (let obj of AC_GAME_OBJECTS) {
        if (!obj.has_called_start) {
            obj.has_called_start = true;
            obj.start();
        } else {
            obj.timedelta = timestamp - last_timestamp;
            obj.update();
        }
    }

    last_timestamp = timestamp;
    requestAnimationFrame(step);
}

requestAnimationFrame(step) // 传入一个函数，那么这个函数就会在下一帧浏览器刷新之前执行一遍，那么如何保持每一帧都执行们就是用递归