package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.data.db.SovereignDatabase
import com.example.data.model.ChronicleEntry
import com.example.data.model.Policy
import com.example.data.model.WorldState
import com.example.data.repository.CharacterOrigin
import com.example.data.repository.GameRepository
import com.example.download.ModelDownloadScheduler
import com.example.download.ModelDownloadWorker
import com.example.engine.llm.EngineStatus
import com.example.engine.orchestrator.GameOrchestrator
import com.example.engine.orchestrator.TurnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class DrawerDestination {
    ACTIONS,
    PERSONAL,
    POLITICS,
    NATION,
    CHRONICLE
}

data class SubCategoryItem(
    val id: String,
    val title: String,
    val parentCategory: DrawerDestination,
    val primaryStat: String,
    val secondaryStat: String,
    val trend: String,
    val isPositiveTrend: Boolean,
    val advisorMemo: String,
    val detailedMetrics: List<Pair<String, String>>,
    val recommendedActions: List<String>
)

class SovereignViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SovereignDatabase.getInstance(application)
    private val repository = GameRepository(db.worldStateDao(), db.chronicleDao(), db.policyDao())

    private val modelDirectory = File(application.filesDir, "models/lfm3")
    val orchestrator = GameOrchestrator(
        db.worldStateDao(),
        db.chronicleDao(),
        db.policyDao(),
        modelDirectory
    )

    val worldState: StateFlow<WorldState?> = repository.worldState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorldState()
    )

    val chronicleEntries: StateFlow<List<ChronicleEntry>> = repository.chronicleEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activePolicies: StateFlow<List<Policy>> = repository.activePolicies.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val turnState: StateFlow<TurnState> = orchestrator.turnState

    // Download & First-Launch States
    private val _isModelReady = MutableStateFlow(ModelDownloadScheduler.hasCompleteModel(application))
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgressPercent = MutableStateFlow(0)
    val downloadProgressPercent: StateFlow<Int> = _downloadProgressPercent.asStateFlow()

    private val _downloadStatusMessage = MutableStateFlow("")
    val downloadStatusMessage: StateFlow<String> = _downloadStatusMessage.asStateFlow()

    // Navigation and Action States
    private val _currentDestination = MutableStateFlow(DrawerDestination.ACTIONS)
    val currentDestination: StateFlow<DrawerDestination> = _currentDestination.asStateFlow()

    private val _selectedSubCategory = MutableStateFlow<SubCategoryItem?>(null)
    val selectedSubCategory: StateFlow<SubCategoryItem?> = _selectedSubCategory.asStateFlow()

    private val _actionText = MutableStateFlow("")
    val actionText: StateFlow<String> = _actionText.asStateFlow()

    private val _isNewGameModalOpen = MutableStateFlow(false)
    val isNewGameModalOpen: StateFlow<Boolean> = _isNewGameModalOpen.asStateFlow()

    val engineStatus: EngineStatus
        get() = orchestrator.engineStatus

    init {
        viewModelScope.launch {
            repository.initializeIfEmpty()
        }
        observeDownloadWork()
    }

    private fun observeDownloadWork() {
        viewModelScope.launch {
            ModelDownloadScheduler.observeDownloadWork(getApplication()).collect { workInfoList ->
                val activeWork = workInfoList.firstOrNull {
                    it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                }

                if (activeWork != null) {
                    _isDownloading.value = true
                    val progress = activeWork.progress
                    val percent = progress.getInt(ModelDownloadWorker.KEY_PROGRESS_PERCENT, _downloadProgressPercent.value)
                    val msg = progress.getString(ModelDownloadWorker.KEY_STATUS_MESSAGE)
                        ?: if (activeWork.state == WorkInfo.State.ENQUEUED) "Queued for download..." else "Downloading model assets..."
                    _downloadProgressPercent.value = percent
                    _downloadStatusMessage.value = msg
                } else {
                    _isDownloading.value = false
                    val failedWork = workInfoList.firstOrNull { it.state == WorkInfo.State.FAILED }
                    if (failedWork != null) {
                        val failMsg = failedWork.outputData.getString(ModelDownloadWorker.KEY_STATUS_MESSAGE)
                            ?: "Download interrupted. Tap Resume to retry or Launch Instant Simulation."
                        _downloadStatusMessage.value = failMsg
                    }
                    val completed = workInfoList.any { it.state == WorkInfo.State.SUCCEEDED }
                    if (completed || ModelDownloadScheduler.hasCompleteModel(getApplication())) {
                        _isModelReady.value = true
                    }
                }
            }
        }
    }

    fun startModelDownload(wifiOnly: Boolean) {
        _isDownloading.value = true
        _downloadStatusMessage.value = "Queuing download task..."
        ModelDownloadScheduler.startDownload(getApplication(), requireUnmetered = wifiOnly)
    }

    fun skipToOfflineSimulation() {
        _isModelReady.value = true
    }

    fun onActionTextChanged(newText: String) {
        _actionText.value = newText
    }

    fun setDrawerDestination(destination: DrawerDestination) {
        _currentDestination.value = destination
        _selectedSubCategory.value = null
    }

    fun selectSubCategory(item: SubCategoryItem?) {
        _selectedSubCategory.value = item
    }

    fun setActionTextAndNavigate(text: String) {
        _actionText.value = text
        _currentDestination.value = DrawerDestination.ACTIONS
        _selectedSubCategory.value = null
    }

    fun runTurn(actionOverride: String? = null) {
        val actionToRun = actionOverride ?: _actionText.value
        viewModelScope.launch {
            orchestrator.executePlayerAction(actionToRun)
            if (actionOverride == null) {
                _actionText.value = ""
            }
        }
    }

    fun advanceWeek() {
        viewModelScope.launch {
            orchestrator.executePlayerAction("")
        }
    }

    fun openNewGameModal() {
        _isNewGameModalOpen.value = true
    }

    fun closeNewGameModal() {
        _isNewGameModalOpen.value = false
    }

    fun startNewGame(origin: CharacterOrigin, nationName: String) {
        viewModelScope.launch {
            repository.startNewGame(origin, nationName)
            _isNewGameModalOpen.value = false
            _currentDestination.value = DrawerDestination.ACTIONS
            _selectedSubCategory.value = null
        }
    }

    fun getQuickActions(state: WorldState?): List<String> {
        if (state == null) return listOf("Hold rally", "Inspect treasury", "Advance week")

        return when (state.lifeStage) {
            "Origin", "Early Life" -> listOf(
                "Hold working-class rally in Oakhaven",
                "Publish op-ed on living wages in state journal",
                "Meet local strike committee delegates",
                "Enroll in night classes on constitutional law",
                "Campaign for municipal district council seat"
            )
            "Entry into Politics" -> listOf(
                "Table municipal ordinance on utility rates",
                "Convene private donor lunch with local merchants",
                "Challenge party chairman on primary nominations",
                "Launch parliamentary bid for industrial constituency",
                "Call press conference exposing council kickbacks"
            )
            "National Stage" -> listOf(
                "Deliver keynote address in parliament on inflation",
                "Negotiate cross-party coalition compromise",
                "Inspect frontline border regiments with General Staff",
                "Propose sovereign green industrial credit fund",
                "Challenge incumbent leadership for prime ministership"
            )
            "Head of State" -> listOf(
                "Authorize nationwide television address on security",
                "Table emergency fiscal stabilization budget",
                "Summon National Security Council on border posture",
                "Reshuffle cabinet to enforce economic discipline",
                "Host summit with foreign trade delegation"
            )
            else -> listOf("Hold rally", "Inspect treasury", "Meet press")
        }
    }

    fun getSubCategories(dest: DrawerDestination, state: WorldState?): List<SubCategoryItem> {
        val s = state ?: WorldState()
        return when (dest) {
            DrawerDestination.PERSONAL -> listOf(
                SubCategoryItem(
                    id = "personal_health",
                    title = "Vitality & Health",
                    parentCategory = DrawerDestination.PERSONAL,
                    primaryStat = "${s.health.toInt()}%",
                    secondaryStat = "Energy: ${s.energy.toInt()}%",
                    trend = if (s.health > 80) "UP" else if (s.health < 60) "DOWN" else "FLAT",
                    isPositiveTrend = s.health >= 70,
                    advisorMemo = "Personal physician reports physical stamina remains robust, though late-night legislative debates are accumulating fatigue.",
                    detailedMetrics = listOf(
                        "Age" to "${s.age} years",
                        "Physical Stamina" to "${s.health.toInt()}/100",
                        "Weekly Energy" to "${s.energy.toInt()}/100",
                        "Stress Resilience" to "High"
                    ),
                    recommendedActions = listOf("Take weekend retreat to restore energy", "Undergo routine executive medical checkup")
                ),
                SubCategoryItem(
                    id = "personal_wealth",
                    title = "Personal Wealth",
                    parentCategory = DrawerDestination.PERSONAL,
                    primaryStat = "$${String.format("%.1f", s.wealthThousands)}k",
                    secondaryStat = "Net Worth",
                    trend = "UP",
                    isPositiveTrend = true,
                    advisorMemo = "Private assets primarily held in domestic sovereign savings bonds and family holdings in Oakhaven.",
                    detailedMetrics = listOf(
                        "Liquid Assets" to "$${String.format("%.1f", s.wealthThousands * 0.4)}k",
                        "Real Estate & Equities" to "$${String.format("%.1f", s.wealthThousands * 0.6)}k",
                        "Legal Blind Trust" to "Compliant",
                        "Donor Shield Fund" to "Active"
                    ),
                    recommendedActions = listOf("Host private fundraising dinner", "Transfer private shares to blind trust")
                ),
                SubCategoryItem(
                    id = "personal_scandals",
                    title = "Scandals & Kompromat Risk",
                    parentCategory = DrawerDestination.PERSONAL,
                    primaryStat = "${s.scandalRisk.toInt()}%",
                    secondaryStat = "Risk Profile",
                    trend = if (s.scandalRisk > 30) "DOWN" else "UP",
                    isPositiveTrend = s.scandalRisk < 30,
                    advisorMemo = "Opposition researchers and investigative tabloids are probing your past campaign donations and youth activism records.",
                    detailedMetrics = listOf(
                        "Current Scandal Index" to "${s.scandalRisk.toInt()}%",
                        "Press Scrutiny Level" to if (s.scandalRisk > 40) "Aggressive" else "Moderate",
                        "Disclosed Assets" to "Fully Audited",
                        "Party Loyalty Buffer" to "${s.reputation.toInt()}%"
                    ),
                    recommendedActions = listOf("Preemptively audit campaign balance sheet", "Instruct legal counsel to issue press warnings")
                ),
                SubCategoryItem(
                    id = "personal_family",
                    title = "Family & Dynasty",
                    parentCategory = DrawerDestination.PERSONAL,
                    primaryStat = s.familyStatus,
                    secondaryStat = "Background: ${s.backgroundOrigin}",
                    trend = "FLAT",
                    isPositiveTrend = true,
                    advisorMemo = "Family ties provide a grounded public narrative that resonates with suburban and working-class voting blocs.",
                    detailedMetrics = listOf(
                        "Household" to s.familyStatus,
                        "Origin Root" to s.backgroundOrigin,
                        "Spouse / Partner" to "Supportive confidante",
                        "Public Sympathy" to "${s.reputation.toInt()}%"
                    ),
                    recommendedActions = listOf("Attend civic family gala", "Protect relatives from political media intrusion")
                )
            )

            DrawerDestination.POLITICS -> listOf(
                SubCategoryItem(
                    id = "politics_parliament",
                    title = "Parliament & Seats",
                    parentCategory = DrawerDestination.POLITICS,
                    primaryStat = "${s.parliamentSeats} / ${s.totalParliamentSeats}",
                    secondaryStat = "Majority Margin",
                    trend = if (s.parliamentSeats >= 251) "UP" else "DOWN",
                    isPositiveTrend = s.parliamentSeats >= 251,
                    advisorMemo = "Governing coalition commands a fragile parliamentary buffer. Backbench dissent on budget bills remains a constant threat.",
                    detailedMetrics = listOf(
                        "Ruling Seats" to "${s.parliamentSeats}",
                        "Opposition Seats" to "${s.totalParliamentSeats - s.parliamentSeats}",
                        "Constitutional Threshold" to "251 seats required",
                        "Whip Discipline" to "${s.coalitionStability.toInt()}%"
                    ),
                    recommendedActions = listOf("Whip party votes on upcoming economic bill", "Offer committee chairmanships to swing MPs")
                ),
                SubCategoryItem(
                    id = "politics_coalition",
                    title = "Coalition Stability",
                    parentCategory = DrawerDestination.POLITICS,
                    primaryStat = "${s.coalitionStability.toInt()}%",
                    secondaryStat = "Pact Health",
                    trend = if (s.coalitionStability > 50) "UP" else "DOWN",
                    isPositiveTrend = s.coalitionStability >= 50,
                    advisorMemo = "Minor coalition partners are demanding regional transit concessions in exchange for voting on state budget appropriations.",
                    detailedMetrics = listOf(
                        "Pact Agreement" to "Valid for current legislative term",
                        "Friction Level" to "${(100 - s.coalitionStability).toInt()}%",
                        "No-Confidence Threat" to if (s.coalitionStability < 40) "Imminent" else "Low"
                    ),
                    recommendedActions = listOf("Convene bilateral coalition leader summit", "Pledge ministerial appointments to junior allies")
                ),
                SubCategoryItem(
                    id = "politics_opposition",
                    title = "Opposition Movement",
                    parentCategory = DrawerDestination.POLITICS,
                    primaryStat = "${s.oppositionStrength.toInt()}%",
                    secondaryStat = "Rival Momentum",
                    trend = if (s.oppositionStrength > 50) "DOWN" else "UP",
                    isPositiveTrend = s.oppositionStrength < 50,
                    advisorMemo = "Opposition party is running hard-hitting television ads targeting youth unemployment and cost of living pressures.",
                    detailedMetrics = listOf(
                        "Opposition Leader" to "Marcus Drake (National Front)",
                        "Shadow Cabinet Aggression" to "High",
                        "Defection Risk" to "Low"
                    ),
                    recommendedActions = listOf("Discredit opposition economic whitepaper", "Co-opt popular opposition proposal into state law")
                ),
                SubCategoryItem(
                    id = "politics_party",
                    title = "Party Grassroots & Faction",
                    parentCategory = DrawerDestination.POLITICS,
                    primaryStat = "${s.reputation.toInt()}%",
                    secondaryStat = s.politicalParty,
                    trend = "UP",
                    isPositiveTrend = true,
                    advisorMemo = "Grassroots party activists are energized by recent speeches, though provincial party leaders demand greater campaign funding.",
                    detailedMetrics = listOf(
                        "Party Apparatus" to s.politicalParty,
                        "Internal Approval" to "${s.reputation.toInt()}%",
                        "Volunteer Cadre" to "14,500 registered canvassers",
                        "Campaign War Chest" to "$4.2M"
                    ),
                    recommendedActions = listOf("Host national party conference", "Distribute campaign war chest to vulnerable districts")
                )
            )

            DrawerDestination.NATION -> listOf(
                SubCategoryItem(
                    id = "nation_economy",
                    title = "Macroeconomy & Treasury",
                    parentCategory = DrawerDestination.NATION,
                    primaryStat = "${if (s.gdpGrowth >= 0) "+" else ""}${String.format("%.1f", s.gdpGrowth)}%",
                    secondaryStat = "GDP Growth (Treasury: $${String.format("%.1f", s.treasuryBillions)}B)",
                    trend = if (s.gdpGrowth > 1.5) "UP" else "DOWN",
                    isPositiveTrend = s.gdpGrowth >= 2.0,
                    advisorMemo = "Central bank holds benchmark rate steady. Exports through maritime terminals are expanding, but consumer debt requires vigilance.",
                    detailedMetrics = listOf(
                        "Annual GDP Growth" to "${s.gdpGrowth}%",
                        "Unemployment Rate" to "${s.unemploymentRate}%",
                        "Consumer Inflation" to "${s.inflationRate}%",
                        "Debt-to-GDP Ratio" to "${s.debtToGdp}%",
                        "Corporate & Income Tax Rate" to "${s.taxRate}%",
                        "Trade Balance" to "+$${s.tradeBalance}B"
                    ),
                    recommendedActions = listOf("Propose targeted sovereign industrial stimulus", "Adjust federal corporate tax rate")
                ),
                SubCategoryItem(
                    id = "nation_society",
                    title = "Society & Public Mood",
                    parentCategory = DrawerDestination.NATION,
                    primaryStat = "${s.publicMood.toInt()}%",
                    secondaryStat = "Protests: ${s.protestIntensity.toInt()}%",
                    trend = if (s.publicMood >= 50) "UP" else "DOWN",
                    isPositiveTrend = s.publicMood >= 50,
                    advisorMemo = "Civic optimism is moderate. While major cities enjoy cultural revitalization, post-industrial towns feel neglected.",
                    detailedMetrics = listOf(
                        "Public Satisfaction" to "${s.publicMood.toInt()}%",
                        "Street Protests Intensity" to "${s.protestIntensity.toInt()}%",
                        "Media Sentiment" to "${s.mediaSentiment.toInt()}%",
                        "Gini Inequality Index" to "${s.inequalityGini}",
                        "Healthcare System Rating" to "${s.healthcareIndex.toInt()}%",
                        "Public Education Index" to "${s.educationIndex.toInt()}%"
                    ),
                    recommendedActions = listOf("Announce regional hospital funding expansion", "Address union delegates on collective bargaining")
                ),
                SubCategoryItem(
                    id = "nation_military",
                    title = "Armed Forces & Security",
                    parentCategory = DrawerDestination.NATION,
                    primaryStat = "${s.militaryReadiness.toInt()}%",
                    secondaryStat = "Defence: $${String.format("%.1f", s.defenceBudgetBillions)}B",
                    trend = if (s.militaryReadiness >= 70) "UP" else "DOWN",
                    isPositiveTrend = s.militaryReadiness >= 70,
                    advisorMemo = "Armed forces maintain high operational posture. Coastal radar and air defense modernization contracts remain on track.",
                    detailedMetrics = listOf(
                        "Combat Readiness" to "${s.militaryReadiness.toInt()}%",
                        "Troop Morale" to "${s.militaryMorale.toInt()}%",
                        "Active Overseas Conflicts" to "${s.ongoingConflictsCount}",
                        "Defence Budget" to "$${s.defenceBudgetBillions}B",
                        "General Staff Alignment" to "${s.militarySupport.toInt()}%"
                    ),
                    recommendedActions = listOf("Commission domestic drone surveillance fleet", "Review officer pay scales to boost retention")
                ),
                SubCategoryItem(
                    id = "nation_factions",
                    title = "Power Factions & Oligarchs",
                    parentCategory = DrawerDestination.NATION,
                    primaryStat = "${((s.militarySupport + s.businessSupport + s.unionsSupport) / 3).toInt()}%",
                    secondaryStat = "Average Consensus",
                    trend = "UP",
                    isPositiveTrend = true,
                    advisorMemo = "Balancing oligarch business lobbies against powerful trade union confederations is the central tightrope of your administration.",
                    detailedMetrics = listOf(
                        "Military Command" to "${s.militarySupport.toInt()}%",
                        "Corporate & Business Elite" to "${s.businessSupport.toInt()}%",
                        "Labor Unions & Syndicates" to "${s.unionsSupport.toInt()}%",
                        "National Press & Media" to "${s.mediaSupport.toInt()}%",
                        "Civil Service & Judiciary" to "${s.civilServiceSupport.toInt()}%"
                    ),
                    recommendedActions = listOf("Convene tripartite economic council", "Grant broadcast license incentives to friendly media")
                )
            )

            else -> emptyList()
        }
    }
}
