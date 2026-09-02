"""Motivational lines and the strict-mode unlock phrase."""
from __future__ import annotations

import random
from datetime import date

QUOTES: list[tuple[str, str]] = [
    ("You will never always be motivated. You have to learn to be disciplined.", ""),
    ("The pain of discipline weighs ounces. The pain of regret weighs tons.", "Jim Rohn"),
    ("We are what we repeatedly do. Excellence, then, is not an act but a habit.", "Will Durant"),
    ("You do not rise to the level of your goals. You fall to the level of your systems.", "James Clear"),
    ("Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln"),
    ("It is not that we have a short time to live, but that we waste a lot of it.", "Seneca"),
    ("The successful warrior is the average person with laser-like focus.", "Bruce Lee"),
    ("Amateurs sit and wait for inspiration. The rest of us just get up and go to work.", "Stephen King"),
    ("Concentrate all your thoughts upon the work at hand.", "Alexander Graham Bell"),
    ("A year from now you may wish you had started today.", "Karen Lamb"),
    ("Nothing will work unless you do.", "Maya Angelou"),
    ("Someone busier than you is studying right now.", ""),
    ("The scroll never ends. That is the whole design. Close it.", ""),
    ("Ten minutes of real study beats two hours of pretending.", ""),
    ("Your future self is watching you right now through memories.", ""),
    ("Do the hard thing while it is still small.", ""),
    ("Motivation gets you started. Habit keeps you going.", "Jim Ryun"),
    ("Every time you say no to a distraction you say yes to yourself.", ""),
    ("Focus is saying no to a hundred good ideas.", "Steve Jobs"),
    ("The best time to plant a tree was 20 years ago. The second best time is now.", ""),
    ("You are one decision away from a completely different day.", ""),
    ("Small daily improvements are the key to staggering long-term results.", ""),
    ("Don't count the days. Make the days count.", "Muhammad Ali"),
    ("Difficult roads often lead to beautiful destinations.", ""),
    ("Energy and persistence conquer all things.", "Benjamin Franklin"),
    ("If it is important to you, you will find a way. If not, you will find an excuse.", ""),
    ("Quality is not an act, it is a habit.", "Aristotle"),
    ("You have power over your mind, not outside events. Realise this and you will find strength.", "Marcus Aurelius"),
    ("Waste no more time arguing what a good person should be. Be one.", "Marcus Aurelius"),
    ("Fall seven times, stand up eight.", "Japanese proverb"),
]

BLOCK_LINES: list[str] = [
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
]

WORDS: list[str] = [
    "anchor", "beacon", "candle", "delta", "ember", "forest", "granite", "harbor",
    "island", "jasper", "kernel", "lantern", "meadow", "north", "orbit", "pillar",
    "quartz", "river", "summit", "timber", "umber", "valley", "willow", "xenon",
    "yonder", "zenith", "amber", "bronze", "cobalt", "dawn", "echo", "flint",
]


def quote_of_the_day() -> tuple[str, str]:
    rng = random.Random(date.today().toordinal())
    return rng.choice(QUOTES)


def random_quote() -> tuple[str, str]:
    return random.choice(QUOTES)


def block_line() -> str:
    return random.choice(BLOCK_LINES)


def unlock_phrase(words: int = 6) -> str:
    return " ".join(random.choice(WORDS) for _ in range(words))
