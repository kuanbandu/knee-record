package com.kuanbandu.kneerecord.engine

import com.kuanbandu.kneerecord.model.Dosage
import com.kuanbandu.kneerecord.model.DoseMode
import com.kuanbandu.kneerecord.model.Exercise
import com.kuanbandu.kneerecord.model.ExerciseCategory
import com.kuanbandu.kneerecord.model.Slot

/**
 * 内置动作库。每个动作都必须写全六个要素：名称、剂量、膝盖口令、作用、安全要点、常见错误，
 * 缺一项会在 App 启动时被 [validateLibrary] 拦下来，避免半成品动作下发到用户面前。
 */
object ExerciseLibrary {

    val ALL: List<Exercise> = listOf(
        // ---------------------------------------------------------- 热身
        Exercise(
            id = "ankle-pump",
            name = "踝泵",
            level = 1,
            category = ExerciseCategory.MOBILITY,
            slot = Slot.WARMUP,
            dosage = Dosage(mode = DoseMode.REPS, reps = 20, sets = 3, restSec = 30),
            kneeCue = "腿伸直垫高，膝盖完全放松不发力",
            target = "促进下肢回流、消肿",
            safety = listOf(
                "仰卧，脚跟下垫一个枕头让膝盖自然伸直",
                "勾脚尖到极限停 2 秒，再绷脚尖到极限停 2 秒算 1 次",
                "全程只有脚踝在动，大腿不要跟着拧",
            ),
            mistakes = listOf("膝盖下面悬空、伸不直", "甩得太快，靠惯性完成"),
        ),
        Exercise(
            id = "heel-slide",
            name = "脚跟滑动",
            level = 1,
            category = ExerciseCategory.MOBILITY,
            slot = Slot.WARMUP,
            dosage = Dosage(mode = DoseMode.REPS, reps = 15, sets = 3, restSec = 30),
            kneeCue = "脚跟贴着床面滑，膝盖放松",
            target = "恢复屈膝角度",
            safety = listOf(
                "仰卧，脚跟不要离开床面",
                "滑到膝盖有牵拉感就停住，停 2 秒再滑回去",
                "可以用毛巾兜住脚踝帮忙带一下",
            ),
            mistakes = listOf("脚跟抬离床面变成抬腿", "忍着疼硬滑"),
            needsFlexion = 80,
        ),
        Exercise(
            id = "quad-set",
            name = "股四头肌绷劲",
            level = 1,
            category = ExerciseCategory.ISOMETRIC,
            slot = Slot.WARMUP,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 10, reps = 8, sets = 3, restSec = 30),
            kneeCue = "膝盖往下压床面，大腿前侧绷紧",
            target = "唤醒股四头肌",
            safety = listOf(
                "仰卧，膝盖下垫一条毛巾卷",
                "绷紧时把脚尖往回勾，膝盖往下压",
                "绷 10 秒松 5 秒为 1 次",
            ),
            mistakes = listOf("憋气发力", "用臀部带动而不是大腿前侧发力"),
        ),
        Exercise(
            id = "patella-mobilization",
            name = "髌骨活动",
            level = 1,
            category = ExerciseCategory.MOBILITY,
            slot = Slot.WARMUP,
            dosage = Dosage(mode = DoseMode.COUNT_UP, seconds = 60, sets = 1, restSec = 15),
            kneeCue = "手只能推动髌骨，不要压着疼的地方",
            target = "改善髌骨活动度",
            safety = listOf(
                "坐位，腿放松伸直",
                "拇指把髌骨往上、下、内、外四个方向轻推",
                "有疼痛就减小幅度，不要硬性对抗",
            ),
            mistakes = listOf("用力过猛把髌骨按疼", "绷着劲做，肌肉不放松"),
        ),

        // ---------------------------------------------------------- 主训练
        Exercise(
            id = "straight-leg-raise",
            name = "直腿抬高",
            level = 1,
            category = ExerciseCategory.ISOMETRIC,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 5, reps = 10, sets = 3, restSec = 45),
            kneeCue = "抬起来之前先把膝盖锁死，脚尖勾住",
            target = "股四头肌、髂腰肌",
            safety = listOf(
                "仰卧，好腿弯曲踩床，患腿完全伸直",
                "抬到与好腿膝盖同高即可，约 30-45 度",
                "抬到顶停 5 秒，再慢慢放下",
            ),
            mistakes = listOf("膝盖没锁死就抬，腿是弯的", "抬太高变成练腰"),
        ),
        Exercise(
            id = "adductor-squeeze",
            name = "大腿内侧夹枕",
            level = 1,
            category = ExerciseCategory.HIP,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 10, reps = 10, sets = 3, restSec = 30),
            kneeCue = "两膝之间夹住，膝盖不要往里翻",
            target = "大腿内侧、臀中肌协同",
            safety = listOf(
                "仰卧，两膝之间夹一个枕头或球",
                "匀速夹紧保持 10 秒，不要憋气",
                "腰背贴住床面，骨盆不抬离",
            ),
            mistakes = listOf("夹得太猛导致内收肌抽筋", "用腰使劲顶床"),
        ),
        Exercise(
            id = "heel-raise",
            name = "提踵",
            level = 1,
            category = ExerciseCategory.ISOMETRIC,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.REPS, reps = 15, sets = 3, restSec = 30),
            kneeCue = "膝盖保持微屈不锁死",
            target = "小腿三头肌",
            safety = listOf(
                "站立，手扶稳固的椅子或墙",
                "慢慢踮起，在最高点停 2 秒再落下",
                "两侧用力要一样多",
            ),
            mistakes = listOf("膝盖完全锁死", "身体前倾借力"),
        ),
        Exercise(
            id = "terminal-knee-extension",
            name = "终末伸膝",
            level = 2,
            category = ExerciseCategory.ISOMETRIC,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 5, reps = 12, sets = 3, restSec = 30),
            kneeCue = "最后 15 度伸直时，膝盖要主动发力锁住",
            target = "股内侧肌、终末伸膝控制",
            safety = listOf(
                "仰卧，膝下垫枕头让膝盖屈约 30 度",
                "慢慢把膝盖伸到完全伸直，停 5 秒",
                "全程脚尖保持回勾",
            ),
            mistakes = listOf("用惯性把腿甩直", "伸到最后膝盖不敢发力"),
        ),
        Exercise(
            id = "glute-bridge",
            name = "臀桥",
            level = 2,
            category = ExerciseCategory.HIP,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.REPS, reps = 15, sets = 3, restSec = 45),
            kneeCue = "脚掌踩实，抬臀时膝盖不要往外翻",
            target = "臀肌、腘绳肌",
            safety = listOf(
                "仰卧，双脚与髋同宽，脚尖朝前",
                "先收紧腹部再抬臀，抬到大腿与躯干成一条线",
                "下落时用 3 秒慢慢放下",
            ),
            mistakes = listOf("腰使劲把身体顶成反弓", "膝盖往外撇"),
        ),
        Exercise(
            id = "clam-shell",
            name = "蚌式开合",
            level = 2,
            category = ExerciseCategory.HIP,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.REPS, reps = 15, sets = 3, restSec = 30),
            kneeCue = "骨盆不许往后倒，两脚保持贴住",
            target = "臀中肌",
            safety = listOf(
                "侧卧，两膝屈约 45 度，两脚并拢",
                "上方膝盖像蚌壳打开一样抬到最大，停 2 秒",
                "骨盆始终保持垂直于地面",
            ),
            mistakes = listOf("身体跟着往后倒", "抬的不是膝盖而是整条腿"),
        ),
        Exercise(
            id = "side-lying-leg-raise",
            name = "侧卧直腿抬高",
            level = 2,
            category = ExerciseCategory.HIP,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 3, reps = 12, sets = 3, restSec = 30),
            kneeCue = "抬腿时膝盖伸直并稍稍后展",
            target = "臀中肌、阔筋膜张肌",
            safety = listOf(
                "侧卧，下方腿微屈保持稳定",
                "上方腿伸直向上抬到约 30 度，停 3 秒",
                "腿不要往前带，保持与躯干在一个平面",
            ),
            mistakes = listOf("抬得过高导致骨盆歪斜", "脚尖往前勾带动身体前旋"),
        ),
        Exercise(
            id = "single-leg-balance",
            name = "单腿站立平衡",
            level = 2,
            category = ExerciseCategory.BALANCE,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 30, reps = 1, sets = 3, restSec = 30),
            kneeCue = "站的那条腿膝盖微屈、对准脚尖方向",
            target = "本体感觉、膝关节稳定",
            safety = listOf(
                "旁边要有个能随手抓稳的支撑物",
                "膝盖微屈约 10 度，不要完全锁死",
                "晃得厉害就先睁眼维持，不要闭眼硬撑",
            ),
            mistakes = listOf("膝盖往内侧塌（膝外翻）", "靠腰左右晃来代偿"),
        ),
        Exercise(
            id = "wall-sit-60",
            name = "靠墙静蹲 60°",
            level = 3,
            category = ExerciseCategory.SQUAT,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 30, reps = 5, sets = 3, restSec = 45),
            kneeCue = "膝盖对准第二根脚趾，不要往内扣",
            target = "股四头肌耐力",
            safety = listOf(
                "背贴墙，脚往前迈到膝盖屈约 60 度",
                "膝盖不超过脚尖，重心落在脚跟",
                "大腿发酸可以提前结束这一秒，不要硬扛",
            ),
            mistakes = listOf("蹲太深超过 90 度", "膝盖往内夹"),
            needsFlexion = 70,
        ),
        Exercise(
            id = "bodyweight-squat",
            name = "自重半蹲",
            level = 3,
            category = ExerciseCategory.SQUAT,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.REPS, reps = 12, sets = 3, restSec = 45),
            kneeCue = "下蹲先屈髋向后坐，膝盖跟着脚尖方向走",
            target = "股四头肌、臀肌、腘绳肌",
            safety = listOf(
                "双脚与肩同宽，脚尖稍微外展",
                "下蹲到约 60 度即可，不要全蹲",
                "起身时脚掌均匀踩实，别把重心压在脚尖",
            ),
            mistakes = listOf("膝盖内扣", "先往前推膝盖再蹲"),
            needsFlexion = 90,
        ),
        Exercise(
            id = "step-up",
            name = "台阶踏上",
            level = 3,
            category = ExerciseCategory.SQUAT,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.REPS, reps = 12, sets = 3, restSec = 45),
            kneeCue = "上台阶时膝盖不能往里塌，要用上面那条腿发力",
            target = "股四头肌、臀肌",
            safety = listOf(
                "台阶高度选择能让膝盖屈约 60-70 度的一档",
                "上去用上面那条腿蹬，下来另一条腿慢慢控住",
                "手扶扶手保持平衡",
            ),
            mistakes = listOf("靠下面那条腿蹬地借力", "下来时直接掉下去"),
            needsFlexion = 90,
        ),
        Exercise(
            id = "reverse-lunge",
            name = "后撤步弓步",
            level = 3,
            category = ExerciseCategory.LUNGE,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.REPS, reps = 10, sets = 3, restSec = 45),
            kneeCue = "前腿膝盖对准脚尖，后腿膝盖垂直下沉",
            target = "股四头肌、臀肌、平衡",
            safety = listOf(
                "往后撤一大步，前膝盖屈约 90 度",
                "后腿膝盖下沉到接近地面但不碰地",
                "躯干保持直立，不要往前压",
            ),
            mistakes = listOf("前膝盖往前冲过脚尖", "步子太小导致膝盖晃动"),
            needsFlexion = 100,
        ),
        Exercise(
            id = "lateral-hop",
            name = "侧向轻跳",
            level = 4,
            category = ExerciseCategory.JUMP,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.REPS, reps = 10, sets = 3, restSec = 60),
            kneeCue = "落地瞬间膝盖必须软着屈，不能直着落地",
            target = "爆发力、落地控制",
            safety = listOf(
                "先在原地上垫一步的重量找感觉",
                "落地时先脚掌再全脚，膝盖微屈吸收冲击",
                "不稳或出现疼痛立刻停止",
            ),
            mistakes = listOf("直着腿落地", "落地后膝盖往里塌"),
            needsFlexion = 60,
        ),
        Exercise(
            id = "squat-jump",
            name = "蹲跳",
            level = 4,
            category = ExerciseCategory.JUMP,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.REPS, reps = 8, sets = 3, restSec = 60),
            kneeCue = "跳起和落地都走同一个膝盖轨迹",
            target = "爆发力、伸膝链",
            safety = listOf(
                "只有在深蹲、静蹲无痛且能轻松完成之后再做",
                "高度不用高，重点是落地那一下要稳",
                "落地后停顿一下再跳下一次，不要连续快跳",
            ),
            mistakes = listOf("连续快跳不给缓冲时间", "落地膝盖内扣"),
            needsFlexion = 100,
        ),
        Exercise(
            id = "full-squat",
            name = "全蹲",
            level = 4,
            category = ExerciseCategory.SQUAT,
            slot = Slot.MAIN,
            dosage = Dosage(mode = DoseMode.REPS, reps = 10, sets = 3, restSec = 60),
            kneeCue = "蹲到底时膝盖放松，不要用弹动起身",
            target = "全范围屈膝控制",
            safety = listOf(
                "需要足够的屈膝活动度，蹲不下去不要硬蹲",
                "脚跟全程踩实，脚跟抬起说明活动度还不够",
                "起身靠臀腿发力，不要用惯性弹",
            ),
            mistakes = listOf("脚跟离地", "蹲到底靠弹性抖一下起来"),
            needsFlexion = 110,
        ),

        // ---------------------------------------------------------- 放松
        Exercise(
            id = "hamstring-stretch",
            name = "腘绳肌拉伸",
            level = 1,
            category = ExerciseCategory.STRETCH,
            slot = Slot.COOLDOWN,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 30, reps = 2, sets = 2, restSec = 20),
            kneeCue = "腿保持微屈，别锁死膝盖硬拉",
            target = "腘绳肌柔韧性",
            safety = listOf(
                "仰卧，用毛巾兜住大腿后侧往上带",
                "拉到大腿后侧有牵拉感即可，不要拉到疼",
                "保持 30 秒，呼吸放慢",
            ),
            mistakes = listOf("用猛力抖着拉", "骨盆跟着抬离床面"),
        ),
        Exercise(
            id = "quad-stretch",
            name = "股四头肌拉伸",
            level = 1,
            category = ExerciseCategory.STRETCH,
            slot = Slot.COOLDOWN,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 30, reps = 2, sets = 2, restSec = 20),
            kneeCue = "另一侧手扶稳，别让膝盖往侧面歪",
            target = "股四头肌柔韧性",
            safety = listOf(
                "侧卧或站立，把脚跟往臀部方向带",
                "骨盆保持中立，不要把腰往前顶",
                "保持 30 秒后慢慢松开",
            ),
            mistakes = listOf("为了拉到位把腰顶成反弓", "手抓脚踝时把脚往外掰"),
            needsFlexion = 100,
        ),
        Exercise(
            id = "calf-stretch",
            name = "小腿拉伸",
            level = 1,
            category = ExerciseCategory.STRETCH,
            slot = Slot.COOLDOWN,
            dosage = Dosage(mode = DoseMode.HOLD, seconds = 30, reps = 2, sets = 2, restSec = 20),
            kneeCue = "后腿膝盖伸直，前腿膝盖屈",
            target = "小腿三头肌柔韧性",
            safety = listOf(
                "弓步靠墙，后腿脚跟踩实",
                "身体往前压到小腿有牵拉感为止",
                "两条腿都要拉",
            ),
            mistakes = listOf("后脚跟抬起来", "身体前倾过度压成腰受力"),
        ),
    )

    /** 疼痛上报时推荐的低强度替代动作 */
    const val SAFE_ALTERNATIVE_ID = "ankle-pump"

    /** 每个环节的目标个数 */
    val QUOTA: Map<Slot, Int> = mapOf(Slot.WARMUP to 2, Slot.MAIN to 4, Slot.COOLDOWN to 2)

    fun byId(id: String): Exercise? = ALL.firstOrNull { it.id == id }

    /**
     * 启动自检：任何一个动作要素不全，直接抛出带清单的错误。
     * 宁可启动失败，也不要把缺要素的动作推给用户。
     */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()
        val ids = mutableSetOf<String>()
        ALL.forEach { e ->
            val tag = "${e.id}(${e.name})"
            if (!ids.add(e.id)) problems.add("$tag 的 id 重复")
            if (e.name.isBlank()) problems.add("$tag 缺少名称")
            if (e.level !in 1..4) problems.add("$tag 等级必须在 1-4")
            if (e.kneeCue.isBlank()) problems.add("$tag 缺少膝盖口令")
            if (e.target.isBlank()) problems.add("$tag 缺少作用说明")
            if (e.safety.size < 2) problems.add("$tag 安全要点少于 2 条")
            if (e.mistakes.isEmpty()) problems.add("$tag 缺少常见错误")
            when (e.dosage.mode) {
                DoseMode.HOLD -> if (e.dosage.seconds <= 0 || e.dosage.reps <= 0) {
                    problems.add("$tag 保持计时缺少时长或次数")
                }
                DoseMode.COUNT_UP -> if (e.dosage.seconds <= 0) problems.add("$tag 正计时缺少时长")
                DoseMode.REPS -> if (e.dosage.reps <= 0) problems.add("$tag 缺少每组次数")
            }
            if (e.dosage.sets <= 0) problems.add("$tag 组数必须大于 0")
        }
        val alt = byId(SAFE_ALTERNATIVE_ID)
        if (alt == null) problems.add("缺少疼痛替代动作 $SAFE_ALTERNATIVE_ID")
        if (Slot.entries.any { (ALL.count { e -> e.slot == it }) < 1 }) {
            problems.add("动作库必须覆盖热身、主训练、放松三个环节")
        }
        return problems
    }
}
