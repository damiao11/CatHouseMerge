# -*- coding: utf-8 -*-
import json

BGM = [
    ("bgm_day", "bgm_day.ogg", 90, 72, "主界面 D1–D18、D22–D24", -18),
    ("bgm_night", "bgm_night.ogg", 80, 60, "阁楼 D19–D21", -18),
    ("bgm_title", "bgm_title.ogg", 40, 56, "启动画面", -18),
    ("sting_chapter", "sting_chapter.ogg", 3, None, "第 6/12/18 天章末", -16),
    ("sting_ending", "sting_ending.ogg", 12, None, "结局", -16),
]

S = [
    ("ui_tap", 0.05, "点击按钮", "轻木鱼，很闷"),
    ("ui_back", 0.08, "返回", "同 tap，音高略低"),
    ("ui_panel_open", 0.15, "打开面板", "纸张展开"),
    ("ui_panel_close", 0.12, "关闭面板", "纸张收拢"),
    ("ui_toggle", 0.06, "开关", "短哒"),
    ("ui_error", 0.20, "不可用操作", "低沉单音"),

    ("item_pick", 0.08, "拖起物品", "布料摩擦"),
    ("item_drop", 0.10, "放下物品", "轻木落桌"),
    ("merge_low", 0.20, "合成 1–2 级", "气泡啵"),
    ("merge_mid", 0.22, "合成 3–4 级", "气泡 + 小铃"),
    ("merge_high", 0.25, "合成 5–6 级", "铃 + 上扬和弦"),
    ("merge_invalid", 0.18, "合成失败", "闷响"),

    ("gen_tap_wood", 0.12, "木材堆点击", "木箱咚"),
    ("gen_tap_metal", 0.12, "工具箱点击", "金属轻碰"),
    ("gen_tap_fabric", 0.12, "布筐点击", "布料噗"),
    ("gen_tap_glass", 0.12, "灯泡盒点击", "玻璃轻响"),
    ("gen_tap_ceramic", 0.12, "陶土袋点击", "陶土闷响"),
    ("gen_tap_plant", 0.12, "种子袋点击", "种子沙沙"),
    ("gen_tap_relic", 0.12, "旧木箱点击", "旧木吱呀"),
    ("gen_upgrade", 0.45, "生成器升级", "咔 + 光晕上扬"),
    ("gen_blocked", 0.15, "生成器无处可放", "短闷响"),

    ("order_ready", 0.50, "订单凑齐", "风铃三音"),
    ("order_complete", 0.80, "交付成功", "硬币 + 猫叫"),
    ("coin_fly", 0.30, "金币飞行", "单枚硬币"),

    ("build_step", 0.40, "建造一步", "锤子两下"),
    ("build_reveal", 1.20, "整间揭幕", "上扬 + 铃"),
    ("build_chapter", 3.00, "章末动画", "复用 sting_chapter"),

    ("store_item", 0.15, "收进仓库", "布袋落地"),
    ("retrieve_item", 0.12, "从仓库取出", "抽出啵"),
    ("warehouse_full", 0.25, "仓库满", "闷响"),
    ("bubble_appear", 0.40, "气泡出现", "闪光上升"),
    ("bubble_place", 0.15, "拖上棋盘", "落位哒"),

    ("cat_orange", 0.50, "大橘", "低沉、有点懒"),
    ("cat_calico", 0.40, "花花", "轻、细、短"),
    ("cat_ragdoll", 0.40, "团子", "软、拖长"),
    ("cat_black", 0.40, "煤球", "短促、不爱搭理"),
    ("cat_tabby", 0.50, "阿狸", "野、亮"),
    ("cat_huzi", 0.50, "虎子", "沙哑、弱"),

    ("text_blip", 0.03, "打字机每 3 字", "极轻"),
    ("dialog_open", 0.20, "对话弹出", "轻呼"),
    ("energy_gain", 0.30, "体力 +1", "上扬小铃"),
    ("energy_empty", 0.30, "体力耗尽", "低沉"),

    ("amb_rain", 20.0, "第 1 天门廊", "循环雨声"),
    ("amb_birds", 15.0, "院子 D16–D18", "循环鸟叫"),
]

def main():
    data = {
        "version": 1,
        "masterLufs": -14,
        "bgmLufs": -18,
        "duckDb": -6,
        "maxStreams": 8,
        "bgm": [{"id": i, "file": f, "seconds": s, "bpm": b,
                 "use": u, "lufs": l, "loop": i.startswith("bgm_")}
                for i, f, s, b, u, l in BGM],
        "sfx": [{"id": i, "file": f"sfx_{i}.ogg", "seconds": s,
                 "trigger": t, "desc": d, "gainDb": -14}
                for i, s, t, d in S],
    }
    with open("audio.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
    print("bgm:", len(BGM), "sfx:", len(S), "total:", len(BGM) + len(S))

main()
