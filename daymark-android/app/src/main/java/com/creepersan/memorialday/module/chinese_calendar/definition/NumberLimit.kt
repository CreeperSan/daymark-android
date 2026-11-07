package com.creepersan.memorialday.module.chinese_calendar.definition

abstract class NumberLimit(val number: Int)


class NumberEqual(number: Int) : NumberLimit(number)

class NumberMoreThan(number: Int) : NumberLimit(number)
class NumberMoreThanOrEqual(number: Int) : NumberLimit(number)

class NumberLessThen(number: Int) : NumberLimit(number)
class NumberLessThenOrEqual(number: Int) : NumberLimit(number)


