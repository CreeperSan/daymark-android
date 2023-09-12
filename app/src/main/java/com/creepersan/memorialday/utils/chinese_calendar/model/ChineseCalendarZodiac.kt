package com.creepersan.memorialday.utils.chinese_calendar.model

enum class ChineseCalendarZodiac(
    val id: Int,
    val nameDisplay: String,
) {

    Unknown(0, "未知"),
    Mouse(1, "鼠"),
    Cow(2, "牛"),
    Tiger(3, "虎"),
    Rabbit(4, "兔"),
    Dragon(5, "龙"),
    Snake(6, "蛇"),
    Horse(7, "马"),
    Sheep(8, "羊"),
    Monkey(9, "猴"),
    Chicken(10, "鸡"),
    Dog(11, "狗"),
    Pig(12, "猪");

    companion object {

        fun fromInt(id: Int): ChineseCalendarZodiac {
            return when (id) {
                1 -> Mouse
                2 -> Cow
                3 -> Tiger
                4 -> Rabbit
                5 -> Dragon
                6 -> Snake
                7 -> Horse
                8 -> Sheep
                9 -> Monkey
                10 -> Chicken
                11 -> Dog
                12 -> Pig
                else -> Unknown
            }
        }

    }


}