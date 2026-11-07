package com.creepersan.memorialday.utils.chinese_calendar.model

enum class ChineseCalendarSolarTerm(
    val id: Int,
    val nameDisplay: String,
) {

    None(0, ""),
    LiChun(1, "立春"),
    YuShui(2, "雨水"),
    JingZhe(3, "惊蛰"),
    ChunFen(4, "春分"),
    QingMing(5, "清明"),
    GuYu(6, "谷雨"),
    LiXia(7, "立夏"),
    XiaoMan(8, "小满"),
    MangZhong(9, "芒种"),
    XiaZhi(10, "夏至"),
    XiaoShu(11, "小暑"),
    DaShu(12, "大暑"),
    LiQiu(13, "立秋"),
    ChuShu(14, "处暑"),
    BaiLu(15, "白露"),
    QiuFen(16, "秋分"),
    HanLu(17, "寒露"),
    ShuangJiang(18, "霜降"),
    LiDong(19, "立冬"),
    XiaoXue(20, "小雪"),
    DaXue(21, "大雪"),
    DongZhi(22, "冬至"),
    XiaoHan(23, "小寒"),
    DaHan(24, "大寒");

    companion object {

        fun fromInt(id: Int): ChineseCalendarSolarTerm {
            return when (id) {
                1 -> LiChun
                2 -> YuShui
                3 -> JingZhe
                4 -> ChunFen
                5 -> QingMing
                6 -> GuYu
                7 -> LiXia
                8 -> XiaoMan
                9 -> MangZhong
                10 -> XiaZhi
                11 -> XiaoShu
                12 -> DaShu
                13 -> LiQiu
                14 -> ChuShu
                15 -> BaiLu
                16 -> QiuFen
                17 -> HanLu
                18 -> ShuangJiang
                19 -> LiDong
                20 -> XiaoXue
                21 -> DaXue
                22 -> DongZhi
                23 -> XiaoHan
                24 -> DaHan
                else -> None
            }
        }

    }

}
