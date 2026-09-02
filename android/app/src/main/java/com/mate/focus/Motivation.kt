package com.mate.focus

import java.util.Calendar
import kotlin.random.Random

data class Quote(val text: String, val author: String)

/** The words Mate says when it gets in your way. */
object Motivation {

    val QUOTES: List<Quote> = listOf(
        Quote("You will never always be motivated. You have to learn to be disciplined.", ""),
        Quote("The pain of discipline weighs ounces. The pain of regret weighs tons.", "Jim Rohn"),
        Quote("We are what we repeatedly do. Excellence, then, is not an act but a habit.", "Will Durant"),
        Quote("You do not rise to the level of your goals. You fall to the level of your systems.", "James Clear"),
        Quote("Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln"),
        Quote("It is not that we have a short time to live, but that we waste a lot of it.", "Seneca"),
        Quote("The successful warrior is the average person with laser-like focus.", "Bruce Lee"),
        Quote("Amateurs sit and wait for inspiration. The rest of us just get up and go to work.", "Stephen King"),
        Quote("Concentrate all your thoughts upon the work at hand.", "Alexander Graham Bell"),
        Quote("A year from now you may wish you had started today.", "Karen Lamb"),
        Quote("Nothing will work unless you do.", "Maya Angelou"),
        Quote("Someone busier than you is studying right now.", ""),
        Quote("The scroll never ends. That is the whole design. Close it.", ""),
        Quote("Ten minutes of real study beats two hours of pretending.", ""),
        Quote("Your future self is watching you right now through memories.", ""),
        Quote("Do the hard thing while it is still small.", ""),
        Quote("Motivation gets you started. Habit keeps you going.", "Jim Ryun"),
        Quote("Every time you say no to a distraction you say yes to yourself.", ""),
        Quote("Focus is saying no to a hundred good ideas.", "Steve Jobs"),
        Quote("The best time to plant a tree was 20 years ago. The second best time is now.", ""),
        Quote("You are one decision away from a completely different day.", ""),
        Quote("Small daily improvements are the key to staggering long-term results.", ""),
        Quote("Don't count the days. Make the days count.", "Muhammad Ali"),
        Quote("Difficult roads often lead to beautiful destinations.", ""),
        Quote("Energy and persistence conquer all things.", "Benjamin Franklin"),
        Quote("If it is important to you, you will find a way. If not, you will find an excuse.", ""),
        Quote("Quality is not an act, it is a habit.", "Aristotle"),
        Quote("You have power over your mind, not outside events. Realise this and you will find strength.", "Marcus Aurelius"),
        Quote("Waste no more time arguing what a good person should be. Be one.", "Marcus Aurelius"),
        Quote("Fall seven times, stand up eight.", "Japanese proverb"),
    )

    val BLOCK_LINES: List<String> = listOf(
        "That is enough for today. Go build something instead.",
        "You set this limit when you were thinking clearly. Trust that version of you.",
        "The feed will still be there tomorrow. Your exam will not wait.",
        "This is the moment the habit either breaks or gets stronger. Your call.",
        "Nothing new is behind that app. You already checked.",
        "Ten more minutes of scrolling changes nothing. Ten minutes of study changes everything.",
        "You are not missing out. You are opting in to your own life.",
        "Close it. Breathe. Open your notes.",
        "The urge passes in about four minutes. Beat the clock.",
        "You did not run out of time. You ran out of the time you gave this.",
    )

    private val WORDS = listOf("anchor", "beacon", "candle", "delta", "ember", "forest", "granite", "harbor", "island", "jasper", "kernel", "lantern", "meadow", "north", "orbit", "pillar", "quartz", "river", "summit", "timber", "umber", "valley", "willow", "xenon", "yonder", "zenith", "amber", "bronze", "cobalt", "dawn", "echo", "flint")

    /** Stable for the whole day, so the home screen does not flicker. */
    fun quoteOfTheDay(): Quote {
        val cal = Calendar.getInstance()
        val seed = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        return QUOTES[seed % QUOTES.size]
    }

    fun randomQuote(): Quote = QUOTES.random()

    fun blockLine(): String = BLOCK_LINES.random()

    /** The phrase strict mode makes you type before it lets go. */
    fun unlockPhrase(words: Int = 6): String =
        (1..words).joinToString(" ") { WORDS[Random.nextInt(WORDS.size)] }
}
