package net.maxsmr.mxstemplate.format

import net.maxsmr.commonutils.format.decoro.*
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.slots.Slot

private val DIGITS_STAR = setOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*'
)

//### - ### - ### - ##
@JvmField
val MASK_SNILS = listOf(
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        hardcodedSpaceSlot(),
        hardcodedHyphenSlot(),
        hardcodedSpaceSlot(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        hardcodedSpaceSlot(),
        hardcodedHyphenSlot(),
        hardcodedSpaceSlot(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        hardcodedSpaceSlot(),
        hardcodedHyphenSlot(),
        hardcodedSpaceSlot(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit()
)

/**
 * нестрогая маска для телефона: допускает "*" после "7" в исходной строке
 */
val MASK_NON_STRICT_PHONE_RUS_NUMBER = listOf(
        hardcodedPlusSlot(),
        setOf('7').toSlot(),
        hardcodedSpaceSlot(),
        hardcodedOpenBracketSlot(),
        DIGITS_STAR.toSlot(),
        DIGITS_STAR.toSlot(),
        DIGITS_STAR.toSlot(),
        hardcodedClosedBracketSlot(),
        hardcodedSpaceSlot(),
        DIGITS_STAR.toSlot(),
        DIGITS_STAR.toSlot(),
        DIGITS_STAR.toSlot(),
        hardcodedSpaceSlot(),
        DIGITS_STAR.toSlot(),
        DIGITS_STAR.toSlot(),
        hardcodedSpaceSlot(),
        DIGITS_STAR.toSlot(),
        DIGITS_STAR.toSlot()
)

val MASK_DATE = listOf(
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.hardcodedSlot('.').withTags(Slot.TAG_DECORATION),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.hardcodedSlot('.').withTags(Slot.TAG_DECORATION),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit())

// endregion