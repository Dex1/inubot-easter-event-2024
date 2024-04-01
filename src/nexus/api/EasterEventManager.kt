package nexus.api

import org.rspeer.game.adapter.component.inventory.Equipment
import org.rspeer.game.adapter.scene.Npc
import org.rspeer.game.component.Interfaces
import org.rspeer.game.component.Inventories
import org.rspeer.game.component.Item
import org.rspeer.game.config.item.entry.builder.ItemEntryBuilder
import org.rspeer.game.config.item.loadout.EquipmentLoadout
import org.rspeer.game.movement.Movement
import org.rspeer.game.movement.pathfinding.Collisions
import org.rspeer.game.movement.pathfinding.Pathing
import org.rspeer.game.position.Position
import org.rspeer.game.position.area.Area
import org.rspeer.game.query.component.ComponentQuery
import org.rspeer.game.query.component.ItemQuery
import org.rspeer.game.query.scene.NpcQuery
import org.rspeer.game.scene.Npcs
import org.rspeer.game.scene.Pickables
import org.rspeer.game.scene.SceneObjects
import javax.inject.Singleton

data class EasterStage(
    val name: String,
    val steps: List<EasterStep>
)

enum class EasterStep {
    // Start event steps
    START_QUEST,
    TALK_TO_UPSET_EASTER_EGG_HUNTER,
    TALK_TO_PROTESTER,
    TALK_TO_DAUGHTER,
    TALK_TO_MOTHER,
    TALK_TO_FATHER,
    TALK_TO_SHIFTY_LOOKING_PRIEST,
    TALK_TO_EGGUS_MANUS,

    // Fetch and read egg book steps, talk again, handle cutscene
    TALK_TO_EGG_PRIEST_ONE,
    TALK_TO_EGG_PRIEST_TWO,
    TALK_TO_EGG_PRIEST_THREE,
    TALK_TO_EGGUS_MANUS_GET_BOOK,
    READ_EGG_BOOK,
    TALK_TO_EGGUS_MANUS_AFTER_READING,
    EASTER_BUNNY_CUTSCENE,

    // Fetch pickaxe, mine 15 eggs, fill cart steps, hand in to bunny
    FETCH_PICKAXE,
    MINE_FIFTEEN_EGGS,
    FILL_CART_ONE,
    FILL_CART_TWO,
    FILL_CART_THREE,
    FILL_CART_FOUR,
    FILL_CART_FIVE,
    HAND_IN_TO_BUNNY_FULL_CARTS,

    // Get access to the crypt, enter it, talk to eg, walk around rooms and interact with entities, finish event
    TALK_TO_EGGUS_MANUS_ENTER_CRYPT,
    ENTER_CRYPT,
    TALK_TO_EG_FETCH_ITEMS,
    CAPTURE_BRAZIER_ROOM,
    CAPTURE_MIDDLE_INTERSECTION_ROOM,
    CAPTURE_MIDDLE_INTERSECTION_WEST_ROOM,
    CAPTURE_MIDDLE_INTERSECTION_NORTH_ROOM,
    LIGHT_UP_JAIL_BRAZIER,
    CAPTURE_JAIL_ROOM,
    CAPTURE_LAST_ROOM,
    INVESTIGATE_PILE_OF_CHOCOLATE,
    DIG_USING_SPADE,
    CAPTURE_CHOCOLATE_ROOM,
    HAND_IN_TO_EG_FULL_SACK,
    LEAVE_CRYPT,
    HAND_IN_QUEST,

    // Exit the script
    COMPLETE_QUEST
}

@Singleton
class EasterEventManager {
    private val logger = Logger("Easter Event Manager")

    private val easterBunnyNpcName = "Easter Bunny"
    private val upsetEasterEggHunterNpcName = "Upset easter egg hunter"
    private val protesterNpcName = "Protester"
    private val disappointedDaughterNpcName = "Disappointed daughter"
    private val disappointedMotherNpcName = "Disappointed mother"
    private val disappointedFatherNpcName = "Disappointed father"
    private val eggusManusNpcName = "Eggus manus"
    private val shiftyLookingPriestNpcName = "Shifty looking priest"
    private val eggPriestNpcName = "Egg Priest"
    private val egNpcName = "Eg"
    private val animatedEggNpcName = "Animated egg"

    private val allNpcChatOptions = arrayOf("Yes")

    private val eggBookItemName = "Yolklore of ayaster"
    private val readItemAction = "Read"
    private val closeInterfaceAction = "Close"
    private val continueInterfaceAction = "Continue"
    private val eggBookInterfaceRootId = 392
    private val eggBookClickContinueInterfaceId = 78
    private val eggBookPageNumberInterfaceId = 10

    private val startQuestPos = Position(3173, 3462, 0)
    private val eggTemplePos = Position(3870, 6115, 0)
    private val eggMineCenterPos = Position(4189, 6104, 0)
    private val eggCryptStartRoomPos = Position(3801, 6087, 0)
    private val jailBrazierPos = Position(3798, 6108, 0)

    private val eggCryptBrazierRoom = Area.rectangular(Position(3792, 6093, 0), Position(3814, 6111, 0))
    private val eggCryptJailSingularRoom = Area.singular(Position(3798, 6107, 0))
    private val eggCryptMiddleIntersectionRoom = Area.rectangular(Position(3814, 6106, 0), Position(3792, 6126, 0))
    private val eggCryptMiddleIntersectionWestRoom = Area.rectangular(Position(3792, 6126, 0), Position(3780, 6112, 0))
    private val eggCryptMiddleIntersectionNorthRoom = Area.rectangular(Position(3792, 6126, 0), Position(3814, 6133, 0))
    private val eggCryptChocolateRoom = Area.rectangular(Position(3827, 6084, 0), Position(3814, 6097, 0))
    private val eggCryptLastRoom = Area.rectangular(Position(3818, 6118, 0), Position(3834, 6132, 0))

    private val eggPriestOnePos = Position(3874, 6117, 0)
    private val eggPriestTwoPos = Position(3864, 6115, 0)
    private val eggPriestThreePos = Position(3877, 6113, 0)

    private val easterMineCartOnePos = Position(4166, 6103, 0)
    private val easterMineCartTwoPos = Position(4176, 6086, 0)
    private val easterMineCartThreePos = Position(4192, 6087, 0)
    private val easterMineCartFourPos = Position(4207, 6092, 0)
    private val easterMineCartFivePos = Position(4209, 6100, 0)

    private val cryptStairCaseWalkTile = Position(3870, 6092, 0)
    private val outsideCryptEntrancePos = Position(3871, 6098, 0)
    private val templeDoorArea = Area.rectangular(Position(3872, 6105, 0), Position(3869, 6106, 0))
    private val cryptStairCaseDoorArea = Area.rectangular(Position(3869, 6096, 0), Position(3873, 6093, 0))

    private val miningArea = Area.rectangular(Position(4168, 6101, 0), Position(4162, 6090, 0))
    private val bronzePickaxeArea = Area.rectangular(Position(4189, 6097, 0), Position(4193, 6094, 0))
    private val takePickableAction = "Take"
    private val bronzePickaxeItemName = "Bronze pickaxe"

    private val easterEggMinedItemName = "Easter egg (mined)"
    private val easterEggRockObjectName = "Easter egg rock"
    private val mineRockAction = "Mine"
    private val validRockId_53063 = 53063
    private val validRockId_53062 = 53062

    private val cryptObjectName = "Crypt staircase"
    private val cryptObjectClimbDownAction = "Climb-down"
    private val cryptObjectClimbUpAction = "Climb-up"

    private val eggSackItemName = "Eggnappin' sack"
    private val spadeItemName = "Spade"
    private val tinderboxItemName = "Tinderbox"

    private val eggPriestOutfit = listOf(
        Pair("Egg priest mitre", Equipment.Slot.HEAD),
        Pair("Egg priest necklace", Equipment.Slot.NECK),
        Pair("Egg priest robe top", Equipment.Slot.CHEST),
        Pair("Egg priest robe", Equipment.Slot.LEGS),
        Pair("Book of egg", Equipment.Slot.OFFHAND)
    )

    private val eggTempleRegionId = 15455
    private val eggMineRegionId = 16735
    private val cryptRegionId = 15199

    private var _currentEasterStep = EasterStep.START_QUEST
    val currentEasterStep get() = _currentEasterStep

    val easterBunnyQuery: NpcQuery
        get() =
            Npcs.query()
                .names(easterBunnyNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstEasterBunny: Npc?
        get() =
            easterBunnyQuery.results().first()

    val upsetEasterEggHunterQuery: NpcQuery
        get() =
            Npcs.query()
                .names(upsetEasterEggHunterNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstUpsetEasterEggHunter: Npc?
        get() =
            upsetEasterEggHunterQuery.results().first()

    val protesterQuery: NpcQuery
        get() =
            Npcs.query()
                .names(protesterNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstProtester: Npc?
        get() =
            protesterQuery.results().first()

    val disappointedDaughterQuery: NpcQuery
        get() =
            Npcs.query()
                .names(disappointedDaughterNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstDisappointedDaughter: Npc?
        get() =
            disappointedDaughterQuery.results().first()

    val disappointedMotherQuery: NpcQuery
        get() =
            Npcs.query()
                .names(disappointedMotherNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstDisappointedMother: Npc?
        get() =
            disappointedMotherQuery.results().first()

    val disappointedFatherQuery: NpcQuery
        get() =
            Npcs.query()
                .names(disappointedFatherNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstDisappointedFather: Npc?
        get() =
            disappointedFatherQuery.results().first()

    val shiftyLookingPriestQuery: NpcQuery
        get() =
            Npcs.query()
                .names(shiftyLookingPriestNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstShiftyLookingPriest: Npc?
        get() = shiftyLookingPriestQuery.results().first()

    val eggusManusQuery: NpcQuery
        get() =
            Npcs.query()
                .names(eggusManusNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstEggusManus: Npc?
        get() =
            eggusManusQuery.results().first()

    val eggPriestOneQuery: NpcQuery
        get() =
            Npcs.query()
                .filter { it.position.equals(eggPriestOnePos) }
                .names(eggPriestNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstEggPriestOne: Npc?
        get() =
            eggPriestOneQuery.results().first()

    val eggPriestTwoQuery: NpcQuery
        get() =
            Npcs.query()
                .filter { it.position.equals(eggPriestTwoPos) }
                .names(eggPriestNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstEggPriestTwo: Npc?
        get() =
            eggPriestTwoQuery.results().first()

    val eggPriestThreeQuery: NpcQuery
        get() =
            Npcs.query()
                .filter { it.position.equals(eggPriestThreePos) }
                .names(eggPriestNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstEggPriestThree: Npc?
        get() =
            eggPriestThreeQuery.results().first()

    val egQuery: NpcQuery
        get() =
            Npcs.query()
                .names(egNpcName)
                .actions(TALK_TO_NPC_ACTION)

    val firstEg: Npc?
        get() =
            egQuery.results().first()

    val animatedEggQuery: NpcQuery
        get() =
            Npcs.query()
                .names(animatedEggNpcName)
                .actions(CAPTURE_NPC_ACTION)

    val firstAnimatedEgg: Npc?
        get() =
            animatedEggQuery.results().first()

    val nearestAnimatedEgg: Npc?
        get() =
            animatedEggQuery.results().nearest()

    val eggBookQuery: ItemQuery
        get() =
            Inventories.backpack()
                .query()
                .names(eggBookItemName)
                .actions(readItemAction)

    val firstEggBook: Item?
        get() =
            eggBookQuery.results().first()

    val eggBookInterfaceQuery: ComponentQuery
        get() =
            Interfaces.query()
                .groups(eggBookInterfaceRootId)
                .visible()

    val eggBookClickContinueInterfaceQuery: ComponentQuery
        get() =
            Interfaces.query()
                .groups(eggBookInterfaceRootId, eggBookClickContinueInterfaceId)
                .visible()

    val eggBookPageNumberInterfaceQuery: ComponentQuery
        get() =
            Interfaces.query()
                .groups(eggBookInterfaceRootId, eggBookPageNumberInterfaceId)
                .visible()

    val eggBookPageNumber: Int?
        get() =
            eggBookPageNumberInterfaceQuery.results()
                .first()
                ?.text
                ?.toIntOrNull()

    val eggBookCloseInterfaceQuery: ComponentQuery
        get() =
            Interfaces.query()
                .groups(eggBookInterfaceRootId)
                .actions(closeInterfaceAction)
                .visible()

    fun incrementStep() {
        val nextOrdinal = _currentEasterStep.ordinal.inc()

        _currentEasterStep = if (nextOrdinal < EasterStep.values().size) {
            EasterStep.values()[nextOrdinal]
        } else {
            EasterStep.COMPLETE_QUEST
        }
    }

    fun manageStartEvent(chatScreenManager: ChatScreenManager): Boolean {
        return walkToStartTile() && firstEasterBunny
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Start Event",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageEggHunter(chatScreenManager: ChatScreenManager): Boolean {
        return firstUpsetEasterEggHunter
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Egg Hunter",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageProtester(chatScreenManager: ChatScreenManager): Boolean {
        return firstProtester
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Protester",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageDaughter(chatScreenManager: ChatScreenManager): Boolean {
        return firstDisappointedDaughter
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Daughter",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageMother(chatScreenManager: ChatScreenManager): Boolean {
        return firstDisappointedMother
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Mother",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageFather(chatScreenManager: ChatScreenManager): Boolean {
        return firstDisappointedFather
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Father",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageShiftyLookingPriest(chatScreenManager: ChatScreenManager): Boolean {
        return firstShiftyLookingPriest
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Shifty Priest",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageEggusManus(
        chatScreenManager: ChatScreenManager,
        action: (Npc) -> Boolean = { it.performTalkToAction() }
    ): Boolean {
        return firstEggusManus
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Eggus Manus",
                    npc,
                    chatScreenManager,
                    action = action
                )
            } ?: false
    }

    fun manageEggPriestOne(chatScreenManager: ChatScreenManager): Boolean {
        return walkToEggTempleTile() && firstEggPriestOne
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Egg Priest One",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageEggPriestTwo(chatScreenManager: ChatScreenManager): Boolean {
        return walkToEggTempleTile() && firstEggPriestTwo
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Egg Priest Two",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageEggPriestThree(chatScreenManager: ChatScreenManager): Boolean {
        return walkToEggTempleTile() && firstEggPriestThree
            ?.let { npc ->
                performNpcActionSequence(
                    "Manage Egg Priest Three",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageEasterBunnyCutscene(chatScreenManager: ChatScreenManager): Boolean {
        logger.debug("Managing easter bunny cutscene")
        return chatScreenManager.isOpen && chatScreenManager.handleChatScreen(*allNpcChatOptions)
    }

    fun manageFetchPickaxe(): Boolean {
        if (hasAnyPickaxe()) {
            return true
        }

        logger.info("Taking Bronze pickaxe from table")
        return walkToBronzePickaxeCenterTile() && takeBronzePickaxe()
    }

    fun manageMineEggRocks(): Boolean {
        if (!miningArea.containsMyPlayer() && !miningArea.randomTile.walkTo()) {
            return false
        }

        if (MyPlayer.isAnimating) {
            return true
        }

        return mineNearestEggRock(miningArea)
    }

    private fun getEasterMineCartQuery(pos: Position) = SceneObjects.query()
        .names("Easter mine cart")
        .filter { it.position.equals(pos) }

    fun manageFillCartOne() = manageFillCart(easterMineCartOnePos)

    fun manageFillCartTwo() = manageFillCart(easterMineCartTwoPos)

    fun manageFillCartThree() = manageFillCart(easterMineCartThreePos)

    fun manageFillCartFour() = manageFillCart(easterMineCartFourPos)

    fun manageFillCartFive() = manageFillCart(easterMineCartFivePos)

    private var shouldMineEggs = false
    private fun manageFillCart(pos: Position): Boolean {
        if (isEasterMineCartFull(pos)) {
            return true
        }

        // if should mine eggs because we got hit by the cart and crashed and broke an egg
        if (shouldMineEggs || !isEasterMineCartFull(pos) && getAllEasterEggsMined().isEmpty()) {
            logger.info("Mining, ran out of eggs to deposit")
            shouldMineEggs = true

            if (hasFifteenEggRocksMined() || isEasterMineCartFull(pos)) {
                shouldMineEggs = false
                return false
            }

            if (MyPlayer.isAnimating) {
                return true
            }

            return mineNearestEggRock()
        }

        if (!Collisions.canReach { pos } && !Movement.walkTo(pos)) {
            return false
        }

        return getEasterMineCartQuery(pos).results()
            .firstOrNull()
            ?.let { cart ->
                if ("Deposit-eggs" !in cart.actions) return@let true
                cart.interact("Deposit-eggs")
            } ?: false
    }

    fun isEasterMineCartOneFull() = isEasterMineCartFull(easterMineCartOnePos)

    fun isEasterMineCartTwoFull() = isEasterMineCartFull(easterMineCartTwoPos)

    fun isEasterMineCartThreeFull() = isEasterMineCartFull(easterMineCartThreePos)

    fun isEasterMineCartFourFull() = isEasterMineCartFull(easterMineCartFourPos)

    fun isEasterMineCartFiveFull() = isEasterMineCartFull(easterMineCartFivePos)

    private fun isEasterMineCartFull(pos: Position) = getEasterMineCartQuery(pos)
        .filter { "Deposit-eggs" !in it.actions && "Check" in it.actions }
        .results()
        .isNotEmpty()

    fun manageHandInBunnyFullCarts(chatScreenManager: ChatScreenManager) =
        walkToEggMineCenterTile() && firstEasterBunny?.let { npc ->
            performNpcActionSequence(
                "Manage Hand In Full Carts",
                npc,
                chatScreenManager
            )
        } ?: false

    private fun openDoor(area: Area) = SceneObjects.query()
        .names("Door")
        .filter { it.position in area.tiles }
        .results()
        .nearest()
        ?.let { door ->
            if (door.actions.contains("Close")) return@let true
            door.interact("Open")
        } ?: false

    fun manageClimbDownCrypt(): Boolean {
        if (isInCryptRegion()) {
            return true
        }

        if (!cryptStairCaseWalkTile.canReach()) {
            if (!outsideCryptEntrancePos.canReach()) {
                if (!openDoor(templeDoorArea)) {
                    logger.info("Failed to open door from temple starting area")
                    return false
                }
                return true
            }

            if (!openDoor(cryptStairCaseDoorArea)) {
                logger.info("Failed to open door outside crypt entrance")
                return false
            }
            return true
        }

        return SceneObjects.query()
            .names(cryptObjectName)
            .actions(cryptObjectClimbDownAction)
            .results()
            .firstOrNull()
            ?.interact(cryptObjectClimbDownAction) ?: false
    }

    fun manageClimbUpCrypt(): Boolean {
        if (isInEggTempleRegion()) {
            return true
        }

        return SceneObjects.query()
            .names(cryptObjectName)
            .actions(cryptObjectClimbUpAction)
            .results()
            .firstOrNull()
            ?.interact(cryptObjectClimbUpAction) ?: false
    }

    fun hasAnyEggOutfitPiece(): Boolean {
        val pieceNames = eggPriestOutfit.map { it.first }

        val invEasterOutfitPieces = Inventories.backpack()
            .query()
            .results()
            .filter { it.name in pieceNames }

        val equipEasterOutfitPieces = Inventories.equipment()
            .query()
            .results()
            .filter { it.name in pieceNames }

        return (invEasterOutfitPieces + equipEasterOutfitPieces).isNotEmpty()
    }

    fun equipAllEggOutfitPieces(): Boolean {
        val loadout = EquipmentLoadout("Egg Outfit")

        eggPriestOutfit.indices.forEach { index ->
            val piece = eggPriestOutfit[index]

            val entry = ItemEntryBuilder()
                .equipmentSlot(piece.second)
                .key(piece.first)
                .quantity(1, 1)
                .build()

            loadout.add(entry)
        }

        loadout.equip()

        return loadout.isWorn
    }

    fun manageHandInQuestToEggusManus(chatScreenManager: ChatScreenManager): Boolean {
        if (hasAnyEggOutfitPiece()) {
            return true
        }

        if (!eggTemplePos.canReach()) {
            if (!outsideCryptEntrancePos.canReach()) {
                if (!openDoor(cryptStairCaseDoorArea)) {
                    logger.info("Failed to open door to exit crypt")
                }
                return false
            }

            if (!openDoor(templeDoorArea)) {
                logger.info("Failed to open door to enter church")
            }
            return false
        }

        if (eggTemplePos.distance() >= 7 && firstEggusManus == null) {
            logger.info("Distance >= 7 to egg temple pos")

            if (!eggTemplePos.walkTo()) {
                logger.info("Failed to walk to temple pos")
                return false
            }

            val walkableTiles = Area.rectangular(Position(3870, 6112, 0), Position(3872, 6123, 0))
            walkableTiles.tiles.firstOrNull { pos ->
                pos.isReachable()
            }?.let { pos ->
                logger.info("Moving towards close position towards eggus manus")
                Movement.walkTowards(pos)
            }
        }

        logger.info("Interacting with eggus man to hand in quest")

        return firstEggusManus
            ?.let { npc ->
                performNpcActionSequence(
                    "Hand In Quest For Completion",
                    npc,
                    chatScreenManager
                )
            } ?: false
    }

    fun manageEgFetchItems(chatScreenManager: ChatScreenManager): Boolean {
        if (hasAllItemsToCapture()) {
            return true
        }

        return walkToEggCryptStartRoom() && firstEg?.let { npc ->
            performNpcActionSequence(
                "Manage Fetch Items",
                npc,
                chatScreenManager
            )
        } ?: false
    }

    private fun useTinderboxOnUnlitJailBrazier() = Inventories.backpack()
        .query()
        .names(tinderboxItemName)
        .unnoted()
        .results()
        .firstOrNull()
        ?.let { tinderbox ->
            SceneObjects.query()
                .filter { it.position.equals(jailBrazierPos) }
                .results()
                .firstOrNull()
                ?.let { unlitBrazier ->
                    Inventories.backpack().use(tinderbox, unlitBrazier)
                }
        } ?: false

    fun manageLightUpJailBrazier(): Boolean {
        if (jailBrazierPos.distance() >= 7) {
            if (eggCryptMiddleIntersectionRoom.tiles
                    .filter { it.distance(jailBrazierPos) <= 5 }
                    .randomOrNull()
                    ?.walkTo() == false
            ) {
                logger.info("Failed to walk to random tile near jail brazier")
                return false
            }
            return true
        }

        if (isRoomClear(eggCryptJailSingularRoom, includeJailRoom = true)) {
            return true
        }

        if (MyPlayer.isAnimating) {
            return true
        }

        // If it becomes lit, then don't attempt to light up the brazier
        if (SceneObjects.query()
                .filter { it.position.equals(jailBrazierPos) }
                .results()
                .firstOrNull()
                ?.let { !it.name.equals("Unlit brazier") } == true
        ) {
            return true
        }

        logger.info("Using tinderbox on unlit brazier beside jail")

        return useTinderboxOnUnlitJailBrazier()
    }

    fun manageChocolateInvestigation(chatScreenManager: ChatScreenManager): Boolean {
        logger.info("Executing: Chocolate Investigation")

        if (!eggCryptChocolateRoom.containsMyPlayer() && !eggCryptChocolateRoom.randomTile.walkTo()) {
            return false
        }

        val obj = SceneObjects.query()
            .names("Pile of chocolate")
            .filter { it.position in eggCryptChocolateRoom.tiles && "Investigate" in it.actions }
            .results()
            .firstOrNull() ?: return false

        if (!obj.canReach() && !obj.walkTo()) {
            return false
        }

        if (!chatScreenManager.isOpen && !obj.interact("Investigate")) {
            return false
        }

        if (obj.distance() >= 2) {
            return false
        }

        if (!chatScreenManager.isOpen) {
            return false
        }

        return chatScreenManager.handleChatScreen(*allNpcChatOptions)
    }

    fun manageUseSpadeDig(): Boolean {
        logger.info("Managing spade usage")

        if (!isRoomClear(eggCryptChocolateRoom)) {
            return true
        }

        return Inventories.backpack()
            .query()
            .names(spadeItemName)
            .unnoted()
            .results()
            .firstOrNull()
            ?.interact("Dig") ?: false
    }

    fun manageHandInEgFullSack(chatScreenManager: ChatScreenManager): Boolean {
        if (!eggCryptStartRoomPos.isNearbyAndReachable() && !eggCryptStartRoomPos.walkTo()) {
            return false
        }

        return firstEg?.let { npc ->
            performNpcActionSequence(
                "Manage Hand In Full Sack",
                npc,
                chatScreenManager
            )
        } ?: false
    }

    fun manageBrazierRoom() = manageRoom("Brazier", room = eggCryptBrazierRoom)

    fun manageMiddleIntersectionRoom() = manageRoom("Middle Intersection", room = eggCryptMiddleIntersectionRoom)

    fun manageMiddleIntersectionWestRoom() = manageRoom("West Intersection", room = eggCryptMiddleIntersectionWestRoom)

    fun manageMiddleIntersectionNorthRoom() =
        manageRoom("North Intersection", room = eggCryptMiddleIntersectionNorthRoom)

    // Same room since we handle lighting the brazier in the step before
    fun manageJailRoom() = manageRoom("Jail/Brazier", room = eggCryptBrazierRoom)

    fun manageLastRoom() = manageRoom("Last", room = eggCryptLastRoom)

    fun manageChocolateRoom() = manageRoom("Chocolate", room = eggCryptChocolateRoom)

    private fun manageRoom(vararg names: String, room: Area): Boolean {
        logger.info("Managing rooms: ${names.joinToString(", ") { it }}")

        if (!(room.containsMyPlayer() || room.randomTile.walkTo())) {
            return false
        }

        if (inRoomAndClear(room)) {
            return true
        }

        logger.info("Interacting with closest animated egg")

        return getAllAnimatedEggsInRooms(room)
            .minByOrNull { it.distance() }
            ?.performCaptureAction() ?: false
    }

    fun inBrazierRoomAndClear() = inRoomAndClear(eggCryptBrazierRoom)

    fun isBrazierRoomClear() = isRoomClear(eggCryptBrazierRoom)

    fun inMiddleIntersectionRoomAndClear() = inRoomAndClear(eggCryptMiddleIntersectionRoom)

    fun inMiddleIntersectionWestRoomAndClear() = inRoomAndClear(eggCryptMiddleIntersectionWestRoom)

    fun inMiddleIntersectionNorthRoomAndClear() = inRoomAndClear(eggCryptMiddleIntersectionNorthRoom)

    fun inLastRoomAndClear() = inRoomAndClear(eggCryptLastRoom)

    fun inChocolateRoomAndClear() = inRoomAndClear(eggCryptChocolateRoom)

    fun isJailSingularRoomClear() = isRoomClear(eggCryptJailSingularRoom, includeJailRoom = true)

    fun inRoomAndClear(
        vararg room: Area,
        includeJailRoom: Boolean = false
    ) = room.any { it.containsMyPlayer() } && isRoomClear(*room, includeJailRoom = includeJailRoom)

    fun isRoomClear(
        vararg room: Area,
        includeJailRoom: Boolean = false
    ) = getAllAnimatedEggsInRooms(
        *room,
        includeJailRoom = includeJailRoom
    ).isEmpty()

    private fun getAllAnimatedEggsInRooms(
        vararg room: Area,
        includeJailRoom: Boolean = false
    ): List<Npc> {
        val npcs = animatedEggQuery.results()
            .filter { npc ->
                room.any { r ->
                    npc.position in r.tiles
                }
            }
            .toMutableList()

        // Remove the jail room position if we are not meant to include it
        if (!includeJailRoom) {
            npcs.removeIf { it.position in eggCryptJailSingularRoom }
        }

        return npcs
    }

    private fun closeEggBook() = eggBookCloseInterfaceQuery.results()
        .firstOrNull()
        ?.interact(closeInterfaceAction) ?: false

    private fun openEggBook(): Boolean {
        if (isEggBookOpen()) return true
        return firstEggBook?.interact(readItemAction) ?: false
    }

    fun hasEggBook() = eggBookQuery.results().isNotEmpty()

    fun isEggBookOpen() = eggBookInterfaceQuery.results().isNotEmpty()

    fun readEggBook() = openEggBook() && closeEggBook()

    private fun getAllPickaxes(): List<Item> {
        val invPickaxes = Inventories.backpack()
            .query()
            .nameContains("pickaxe")
            .unnoted()
            .results()
            .toList()

        val equipPickaxes = Inventories.equipment()
            .query()
            .nameContains("pickaxe")
            .results()
            .toList()

        return invPickaxes + equipPickaxes
    }

    fun hasAnyPickaxe() = getAllPickaxes().isNotEmpty()

    private fun takeBronzePickaxe() = Pickables.query()
        .names(bronzePickaxeItemName)
        .unnoted()
        .filter { it.distance() <= 10 }
        .results()
        .firstOrNull()
        ?.interact(takePickableAction) ?: false

    private fun getAllEasterEggsMined() = Inventories.backpack()
        .query()
        .names(easterEggMinedItemName)
        .unnoted()
        .results()
        .toList()

    fun hasFifteenEggRocksMined() = getAllEasterEggsMined().count() >= 15

    private fun getEasterEggRockQuery() = SceneObjects.query()
        .ids(validRockId_53062, validRockId_53063)

    private fun mineNearestEggRock(area: Area? = null) = getEasterEggRockQuery()
        .filter { area != null && it.position in area.tiles }
        .results()
        .nearest()
        ?.interact(mineRockAction) ?: false

    fun hasAllItemsToCapture() = Inventories.backpack()
        .query()
        .unnoted()
        .results()
        .toList()
        .map { it.name }
        .let {
            eggSackItemName in it && spadeItemName in it && tinderboxItemName in it
        }

    fun isInEggMineRegion() = MyPlayer.withinRegions(eggMineRegionId)

    fun isInEggTempleRegion() = MyPlayer.withinRegions(eggTempleRegionId)

    fun isInCryptRegion() = MyPlayer.withinRegions(cryptRegionId)

    private fun walkToStartTile(): Boolean {
        if (startQuestPos.isNearbyAndReachable()) {
            return true
        }

        return startQuestPos.walkTo()
    }

    private fun walkToEggTempleTile(): Boolean {
        if (eggTemplePos.isNearbyAndReachable()) {
            return true
        }

        return eggTemplePos.walkTo()
    }

    private fun walkToEggMineCenterTile(): Boolean {
        if (eggMineCenterPos.isNearbyAndReachable()) {
            return true
        }

        return eggMineCenterPos.walkTo()
    }

    private fun walkToBronzePickaxeCenterTile(): Boolean {
        if (MyPlayer.position in bronzePickaxeArea.tiles && bronzePickaxeArea.center.distance() <= 2) {
            return true
        }

        return bronzePickaxeArea.center.walkTo() && bronzePickaxeArea.center.distance() <= 2
    }

    private fun walkToEggCryptStartRoom(): Boolean {
        if (eggCryptStartRoomPos.isNearbyAndReachable()) {
            return true
        }

        return eggCryptStartRoomPos.walkTo()
    }

    private fun performNpcActionSequence(
        actionName: String,
        npc: Npc,
        chatScreenManager: ChatScreenManager,
        vararg options: String = allNpcChatOptions,
        action: (Npc) -> Boolean = { it.performTalkToAction() }
    ): Boolean {
        logger.info("Executing: $actionName")

        if (!npc.canReach() && !npc.walkTo()) {
            return false
        }

        if (!chatScreenManager.isOpen && !action.invoke(npc)) {
            return false
        }

        if (npc.distance() >= 2) {
            return false
        }

        if (!chatScreenManager.isOpen) {
            return false
        }

        return chatScreenManager.handleChatScreen(*options)
    }
}
