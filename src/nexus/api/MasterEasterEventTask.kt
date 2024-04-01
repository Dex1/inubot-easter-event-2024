package nexus.api

import org.rspeer.game.Cutscene
import org.rspeer.game.Game
import org.rspeer.game.component.tdi.SubTab.Settings
import org.rspeer.game.movement.Movement
import org.rspeer.game.script.Task
import org.rspeer.game.script.TaskDescriptor
import javax.inject.Inject

private const val TASK_NAME = "Master Easter Event"

@TaskDescriptor(name = TASK_NAME)
class MasterEasterEventTask @Inject constructor(
    private val easterEventManager: EasterEventManager,
    private val antibanManager: AntibanManager,
    private val chatScreenManager: ChatScreenManager
) : Task() {
    private val logger = Logger(TASK_NAME)
    private var actionResult = false

    private val startEventStage = EasterStage(
        "Start Event", listOf(
            EasterStep.START_QUEST,
            EasterStep.TALK_TO_UPSET_EASTER_EGG_HUNTER,
            EasterStep.TALK_TO_PROTESTER,
            EasterStep.TALK_TO_DAUGHTER,
            EasterStep.TALK_TO_MOTHER,
            EasterStep.TALK_TO_FATHER,
            EasterStep.TALK_TO_SHIFTY_LOOKING_PRIEST,
            EasterStep.TALK_TO_EGGUS_MANUS
        )
    )

    private val fetchAndReadEggBookStage = EasterStage(
        "Fetch and Read Egg Book", listOf(
            EasterStep.TALK_TO_EGG_PRIEST_ONE,
            EasterStep.TALK_TO_EGG_PRIEST_TWO,
            EasterStep.TALK_TO_EGG_PRIEST_THREE,
            EasterStep.TALK_TO_EGGUS_MANUS_GET_BOOK,
            EasterStep.READ_EGG_BOOK,
            EasterStep.TALK_TO_EGGUS_MANUS_AFTER_READING,
            EasterStep.EASTER_BUNNY_CUTSCENE
        )
    )

    private val mineEggsAndFillCartsStage = EasterStage(
        "Mine Eggs And Fill 5 Carts Full", listOf(
            EasterStep.FETCH_PICKAXE,
            EasterStep.MINE_FIFTEEN_EGGS,
            EasterStep.FILL_CART_ONE,
            EasterStep.FILL_CART_TWO,
            EasterStep.FILL_CART_THREE,
            EasterStep.FILL_CART_FOUR,
            EasterStep.FILL_CART_FIVE,
            EasterStep.HAND_IN_TO_BUNNY_FULL_CARTS
        )
    )

    private val finalStage = EasterStage(
        "Final Stage -> Enter crypt, enter rooms and interact with objects", listOf(
            EasterStep.TALK_TO_EGGUS_MANUS_ENTER_CRYPT,
            EasterStep.ENTER_CRYPT,
            EasterStep.TALK_TO_EG_FETCH_ITEMS,
            EasterStep.CAPTURE_BRAZIER_ROOM,
            EasterStep.CAPTURE_MIDDLE_INTERSECTION_ROOM,
            EasterStep.CAPTURE_MIDDLE_INTERSECTION_WEST_ROOM,
            EasterStep.CAPTURE_MIDDLE_INTERSECTION_NORTH_ROOM,
            EasterStep.LIGHT_UP_JAIL_BRAZIER,
            EasterStep.CAPTURE_JAIL_ROOM,
            EasterStep.CAPTURE_LAST_ROOM,
            EasterStep.INVESTIGATE_PILE_OF_CHOCOLATE,
            EasterStep.DIG_USING_SPADE,
            EasterStep.CAPTURE_CHOCOLATE_ROOM,
            EasterStep.HAND_IN_TO_EG_FULL_SACK,
            EasterStep.LEAVE_CRYPT,
            EasterStep.HAND_IN_QUEST,
            EasterStep.COMPLETE_QUEST
        )
    )

    override fun execute(): Boolean {
        val currentStep = easterEventManager.currentEasterStep
        if (currentStep == EasterStep.COMPLETE_QUEST) return true

        sleep(1)

        antibanManager.checkAndUpdateProps()

        if (MyPlayer.isMoving) {
            if (antibanManager.shouldToggleOnRun()) {
                Movement.toggleRun(true)
            }
            return true
        }

        logger.info("Current step: ${currentStep.ordinal} -> $currentStep")

        val currentPos = MyPlayer.position
        executeStep(currentStep)

        when {
            !actionResult -> return false

            currentStep in startEventStage.steps -> {
                // Check if chat screen is open and last step was part of the starting stage
                if (chatScreenManager.isOpen) {
                    logger.info("Chat screen still open")
                    sleepUntil({ !chatScreenManager.isOpen }, 1)
                }

                // Update step and perform additional checks if needed
                if (!chatScreenManager.isOpen) {
                    updateStates()

                    // Additional actions after updating step, if required
                    if (currentStep == EasterStep.TALK_TO_EGGUS_MANUS) {
                        logger.info("Sleeping until last position dist >= 7")
                        sleepUntil({ currentPos.distance() >= 7 }, 7)
                    }

                    return true
                }
            }

            currentStep in fetchAndReadEggBookStage.steps -> {
                if (chatScreenManager.isOpen) {
                    logger.info("Chat screen still open")
                    sleepUntil({ !chatScreenManager.isOpen }, 1)
                }

                if (currentStep != EasterStep.TALK_TO_EGGUS_MANUS_GET_BOOK
                    && currentStep != EasterStep.READ_EGG_BOOK
                    && currentStep != EasterStep.EASTER_BUNNY_CUTSCENE
                    && !chatScreenManager.isOpen
                ) {
                    logger.info("Managed chat screen successfully")

                    updateStates()

                    if (currentStep == EasterStep.TALK_TO_EGGUS_MANUS_AFTER_READING) {
                        logger.info("Sleeping until in cutscene and last position dist >= 7")
                        sleepUntil({ Cutscene.isActive() && currentPos.distance() >= 7 }, 7)
                    }

                    return true
                }

                if (!easterEventManager.hasEggBook()) {
                    logger.info("Egg book not found yet")
                    sleepUntil({ easterEventManager.hasEggBook() }, 1)
                }

                if (currentStep == EasterStep.TALK_TO_EGGUS_MANUS_GET_BOOK && easterEventManager.hasEggBook()) {
                    logger.info("Got egg book successfully")
                    updateStates()
                    return true
                }

                if (currentStep == EasterStep.READ_EGG_BOOK && easterEventManager.isEggBookOpen()) {
                    logger.info("Egg book is still open")
                    sleepUntil({ !easterEventManager.isEggBookOpen() }, 1)
                }

                if (currentStep == EasterStep.READ_EGG_BOOK && !easterEventManager.isEggBookOpen()) {
                    logger.info("Managed egg book reading successfully")
                    updateStates()
                    return true
                }

                if (currentStep == EasterStep.EASTER_BUNNY_CUTSCENE
                    && !Cutscene.isActive()
                    && !chatScreenManager.isOpen
                    && easterEventManager.isInEggMineRegion()
                ) {
                    logger.debug("Managed easter bunny cutscene successfully")
                    updateStates()
                    return true
                }
            }

            currentStep in mineEggsAndFillCartsStage.steps -> {
                when (currentStep) {
                    EasterStep.FETCH_PICKAXE -> {
                        if (easterEventManager.hasAnyPickaxe()) {
                            logger.info("Managed to fetch pickaxe successfully")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.MINE_FIFTEEN_EGGS -> {
                        if (easterEventManager.hasFifteenEggRocksMined()) {
                            logger.info("Managed to mine 15 egg rocks")
                            updateStates()
                            return true
                        }
                    }

                    else -> {
                        if (currentStep == EasterStep.FILL_CART_ONE && easterEventManager.isEasterMineCartOneFull()) {
                            logger.info("Detected mine cart one full")
                            updateStates()
                            return true
                        }

                        if (currentStep == EasterStep.FILL_CART_TWO && easterEventManager.isEasterMineCartTwoFull()) {
                            logger.info("Detected mine cart two full")
                            updateStates()
                            return true
                        }

                        if (currentStep == EasterStep.FILL_CART_THREE && easterEventManager.isEasterMineCartThreeFull()) {
                            logger.info("Detected mine cart three full")
                            updateStates()
                            return true
                        }

                        if (currentStep == EasterStep.FILL_CART_FOUR && easterEventManager.isEasterMineCartFourFull()) {
                            logger.info("Detected mine cart four full")
                            updateStates()
                            return true
                        }

                        if (currentStep == EasterStep.FILL_CART_FIVE && easterEventManager.isEasterMineCartFiveFull()) {
                            logger.info("Detected mine cart five full")
                            updateStates()
                            return true
                        }

                        if (currentStep == EasterStep.HAND_IN_TO_BUNNY_FULL_CARTS) {
                            if (chatScreenManager.isOpen) {
                                logger.info("Chat screen still open")
                                sleepUntil({ !chatScreenManager.isOpen }, 1)
                            }

                            if (easterEventManager.isInEggTempleRegion()) {
                                updateStates()
                                logger.info("Detected region change")
                                return true
                            }
                        }
                    }
                }
            }

            currentStep in finalStage.steps -> {
                when (currentStep) {
                    EasterStep.TALK_TO_EGGUS_MANUS_ENTER_CRYPT -> {
                        // Check if chat screen is open and last step was part of the starting stage
                        if (chatScreenManager.isOpen) {
                            logger.info("Chat screen still open")
                            sleepUntil({ !chatScreenManager.isOpen }, 1)
                        }

                        // Update step and perform additional checks if needed
                        if (!chatScreenManager.isOpen) {
                            logger.info("Managed talk to eggus manus for entering the crypt")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.ENTER_CRYPT -> {
                        if (!MyPlayer.isMoving) {
                            logger.info("Sleeping until player movement detected")
                            sleepUntil({ MyPlayer.isMoving }, 1)
                        }

                        if (easterEventManager.isInCryptRegion()) {
                            logger.info("Detected region change")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.TALK_TO_EG_FETCH_ITEMS -> {
                        if (chatScreenManager.isOpen) {
                            logger.info("Chat screen still open")
                            sleepUntil({ !chatScreenManager.isOpen }, 1)
                        }

                        if (easterEventManager.hasAllItemsToCapture()) {
                            logger.info("Managed to fetch all required items for capturing")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.CAPTURE_BRAZIER_ROOM -> {
                        if (easterEventManager.inBrazierRoomAndClear()) {
                            logger.info("Detected brazier room cleared")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.CAPTURE_MIDDLE_INTERSECTION_ROOM -> {
                        if (easterEventManager.inMiddleIntersectionRoomAndClear()) {
                            logger.info("Detected middle intersection room cleared")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.CAPTURE_MIDDLE_INTERSECTION_WEST_ROOM -> {
                        if (easterEventManager.inMiddleIntersectionWestRoomAndClear()) {
                            logger.info("Detected middle intersection west room cleared")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.CAPTURE_MIDDLE_INTERSECTION_NORTH_ROOM -> {
                        if (easterEventManager.inMiddleIntersectionNorthRoomAndClear()) {
                            logger.info("Detected middle intersection north room cleared")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.LIGHT_UP_JAIL_BRAZIER -> {
                        if (easterEventManager.isJailSingularRoomClear() && !easterEventManager.isBrazierRoomClear()) {
                            logger.info("Detected jail cleared")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.CAPTURE_JAIL_ROOM -> {
                        if (easterEventManager.inBrazierRoomAndClear() && easterEventManager.isJailSingularRoomClear()) {
                            logger.info("Detected jail/brazier room cleared")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.CAPTURE_LAST_ROOM -> {
                        if (easterEventManager.inLastRoomAndClear()) {
                            logger.info("Detected last room cleared")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.INVESTIGATE_PILE_OF_CHOCOLATE -> {
                        if (chatScreenManager.isOpen) {
                            logger.info("Chat screen still open")
                            sleepUntil({ !chatScreenManager.isOpen }, 1)
                        }

                        if (!chatScreenManager.isOpen) {
                            logger.info("Managed chocolate pile investigation interface")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.DIG_USING_SPADE -> {
                        if (!easterEventManager.inChocolateRoomAndClear()) {
                            logger.info("Detected animated egg spawn")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.CAPTURE_CHOCOLATE_ROOM -> {
                        if (easterEventManager.inChocolateRoomAndClear()) {
                            logger.info("Detected chocolate room cleared")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.HAND_IN_TO_EG_FULL_SACK -> {
                        if (chatScreenManager.isOpen) {
                            logger.info("Chat screen still open")
                            sleepUntil({ !chatScreenManager.isOpen }, 1)
                        }

                        if (!chatScreenManager.isOpen) {
                            logger.info("Managed to hand in the full sack to eg")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.LEAVE_CRYPT -> {
                        if (easterEventManager.isInEggTempleRegion()) {
                            logger.info("Detected region change")
                            updateStates()
                            return true
                        }
                    }

                    EasterStep.HAND_IN_QUEST -> {
                        if (chatScreenManager.isOpen) {
                            logger.info("Chat screen still open")
                            sleepUntil({ !chatScreenManager.isOpen }, 1)
                        }

                        if (!chatScreenManager.isOpen && easterEventManager.hasAnyEggOutfitPiece()) {
                            logger.info("Managed to hand in the quest to eggus manus")
                            logger.info("Detected quest complete")
                            updateStates()
                            return true
                        }
                    }

                    else -> {
                        // Do nothing, all steps are complete
                    }
                }
            }
        }

        return actionResult
    }

    private fun updateStates() {
        logger.info("Update step")
        easterEventManager.incrementStep()
        logger.info("Current step: ${easterEventManager.currentEasterStep}")
        actionResult = false
    }

    private fun isChatStepValid() = !actionResult || chatScreenManager.isOpen

    private fun executeStep(step: EasterStep) {
        when (step) {
            EasterStep.START_QUEST -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageStartEvent(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_UPSET_EASTER_EGG_HUNTER -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEggHunter(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_PROTESTER -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageProtester(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_DAUGHTER -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageDaughter(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_MOTHER -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageMother(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_FATHER -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageFather(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_SHIFTY_LOOKING_PRIEST -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageShiftyLookingPriest(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_EGGUS_MANUS -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEggusManus(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_EGG_PRIEST_ONE -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEggPriestOne(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_EGG_PRIEST_TWO -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEggPriestTwo(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_EGG_PRIEST_THREE -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEggPriestThree(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_EGGUS_MANUS_GET_BOOK -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEggusManus(chatScreenManager)
                }
            }

            EasterStep.READ_EGG_BOOK -> {
                if (!actionResult || easterEventManager.isEggBookOpen()) {
                    actionResult = easterEventManager.readEggBook()
                }
            }

            EasterStep.TALK_TO_EGGUS_MANUS_AFTER_READING -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEggusManus(chatScreenManager)
                }
            }

            EasterStep.EASTER_BUNNY_CUTSCENE -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEasterBunnyCutscene(chatScreenManager)
                }
            }

            EasterStep.FETCH_PICKAXE -> {
                actionResult = easterEventManager.manageFetchPickaxe()
            }

            EasterStep.MINE_FIFTEEN_EGGS -> {
                actionResult = easterEventManager.manageMineEggRocks()
            }

            EasterStep.FILL_CART_ONE -> {
                actionResult = easterEventManager.manageFillCartOne()
            }

            EasterStep.FILL_CART_TWO -> {
                actionResult = easterEventManager.manageFillCartTwo()
            }

            EasterStep.FILL_CART_THREE -> {
                actionResult = easterEventManager.manageFillCartThree()
            }

            EasterStep.FILL_CART_FOUR -> {
                actionResult = easterEventManager.manageFillCartFour()
            }

            EasterStep.FILL_CART_FIVE -> {
                actionResult = easterEventManager.manageFillCartFive()
            }

            EasterStep.HAND_IN_TO_BUNNY_FULL_CARTS -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageHandInBunnyFullCarts(chatScreenManager)
                }
            }

            EasterStep.TALK_TO_EGGUS_MANUS_ENTER_CRYPT -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEggusManus(chatScreenManager)
                }
            }

            EasterStep.ENTER_CRYPT -> {
                actionResult = easterEventManager.manageClimbDownCrypt()
            }

            EasterStep.TALK_TO_EG_FETCH_ITEMS -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageEgFetchItems(chatScreenManager)
                }
            }

            EasterStep.CAPTURE_BRAZIER_ROOM -> {
                actionResult = easterEventManager.manageBrazierRoom()
            }

            EasterStep.CAPTURE_MIDDLE_INTERSECTION_ROOM -> {
                actionResult = easterEventManager.manageMiddleIntersectionRoom()
            }

            EasterStep.CAPTURE_MIDDLE_INTERSECTION_WEST_ROOM -> {
                actionResult = easterEventManager.manageMiddleIntersectionWestRoom()
            }

            EasterStep.CAPTURE_MIDDLE_INTERSECTION_NORTH_ROOM -> {
                actionResult = easterEventManager.manageMiddleIntersectionNorthRoom()
            }

            EasterStep.LIGHT_UP_JAIL_BRAZIER -> {
                actionResult = easterEventManager.manageLightUpJailBrazier()
            }

            EasterStep.CAPTURE_JAIL_ROOM -> {
                actionResult = easterEventManager.manageJailRoom()
            }

            EasterStep.CAPTURE_LAST_ROOM -> {
                actionResult = easterEventManager.manageLastRoom()
            }

            EasterStep.INVESTIGATE_PILE_OF_CHOCOLATE -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageChocolateInvestigation(chatScreenManager)

                }
            }

            EasterStep.DIG_USING_SPADE -> {
                actionResult = easterEventManager.manageUseSpadeDig()
            }

            EasterStep.CAPTURE_CHOCOLATE_ROOM -> {
                actionResult = easterEventManager.manageChocolateRoom()
            }

            EasterStep.HAND_IN_TO_EG_FULL_SACK -> {
                if (isChatStepValid()) {
                    actionResult = easterEventManager.manageHandInEgFullSack(chatScreenManager)
                }
            }

            EasterStep.LEAVE_CRYPT -> {
                actionResult = easterEventManager.manageClimbUpCrypt()
            }

            EasterStep.HAND_IN_QUEST -> {
                actionResult = easterEventManager.manageHandInQuestToEggusManus(chatScreenManager)
            }

            EasterStep.COMPLETE_QUEST -> {
                // Do nothing, event is complete
            }
        }
    }
}