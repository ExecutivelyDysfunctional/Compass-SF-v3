package com.example.data

object SeedData {

    private fun h(days: List<Int>, open: String, close: String, label: String? = null): List<HourBlock> {
        return days.map { Day -> HourBlock(day = Day, open = open, close = close, label = label) }
    }

    val ALL = listOf(0, 1, 2, 3, 4, 5, 6)
    val WEEK = listOf(1, 2, 3, 4, 5)
    val MON_SAT = listOf(1, 2, 3, 4, 5, 6)

    val SEED_RESOURCES = listOf(
        Resource(
            name = "GLIDE — Daily Free Meals",
            category = "food",
            alsoOffers = listOf("community", "shelter", "documents"),
            summary = "Three free meals a day, every day, no questions asked. Also a walk-in center for case management and access to the housing queue.",
            description = "GLIDE serves breakfast, lunch and dinner 365 days a year in the Tenderloin. Nobody is turned away, no ID needed. The Walk-In Center upstairs can help with benefits, hygiene kits and referrals, and GLIDE is an Adult Coordinated Entry Access Point (how you get on the housing waitlist).",
            address = "330 Ellis St",
            neighborhood = "Tenderloin",
            phone = "(415) 674-6000",
            website = "https://www.glide.org",
            hoursText = "Daily: breakfast 8–9am, lunch 12–1:30pm, dinner 4–5:30pm",
            hours = h(ALL, "08:00", "09:00", "breakfast") +
                    h(ALL, "12:00", "13:30", "lunch") +
                    h(ALL, "16:00", "17:30", "dinner"),
            requirements = listOf("No ID required", "Walk-ins welcome", "First come, first served — line up early"),
            tags = listOf("food", "meals", "tenderloin", "no-id", "access point"),
            cost = "Free",
            createdVia = "seed",
            aiTips = "Lines are shortest right at the start of service. The dinner line forms on Ellis; the breakfast line moves fast."
        ),
        Resource(
            name = "St. Anthony's Dining Room",
            category = "food",
            alsoOffers = listOf("hygiene", "health", "connect"),
            summary = "Free hot lunch every day, plus a free clothing program, tech lab and medical clinic on the same block.",
            description = "One of the oldest free dining rooms in the city. Sit-down lunch daily. The Free Clothing Program (150 Golden Gate) gives out a full outfit; the Tech Lab has free computers and phone charging; the medical clinic serves people without insurance.",
            address = "121 Golden Gate Ave",
            neighborhood = "Tenderloin",
            phone = "(415) 592-2726",
            website = "https://www.stanthonysf.org",
            hoursText = "Dining room daily 11:30am–1:30pm; clothing & tech lab weekdays 9am–3pm",
            hours = h(ALL, "11:30", "13:30", "lunch") + h(WEEK, "09:00", "15:00", "drop-in"),
            requirements = listOf("No ID required", "Walk-ins welcome"),
            tags = listOf("food", "clothing", "computers", "tenderloin"),
            cost = "Free",
            createdVia = "seed",
            aiTips = "Ask at the front about the clothing program — it's a separate door around the corner."
        ),
        Resource(
            name = "Martin de Porres House of Hospitality",
            category = "food",
            alsoOffers = listOf("hygiene"),
            summary = "Quiet, dignified free breakfast and lunch. Showers some mornings.",
            description = "A small Catholic Worker soup kitchen with a calmer feel than the big Tenderloin sites. Vegetarian meals, table service.",
            address = "225 Potrero Ave",
            neighborhood = "SoMa",
            phone = "(415) 552-0240",
            hoursText = "Mon–Sat breakfast 6:30–8am; Tue–Sat lunch 12–2pm",
            hours = h(MON_SAT, "06:30", "08:00", "breakfast") + h(listOf(2, 3, 4, 5, 6), "12:00", "14:00", "lunch"),
            requirements = listOf("No ID required"),
            tags = listOf("food", "breakfast", "soma", "quiet"),
            cost = "Free",
            createdVia = "seed"
        ),
        Resource(
            name = "SF-Marin Food Bank — Pantry Line",
            phoneLine = true,
            category = "food",
            summary = "Call or go online to find the free grocery pantry closest to where you are.",
            description = "The Food Bank runs hundreds of free neighborhood pantries across SF. You get a bag of groceries — produce, rice, beans, eggs. Most pantries just need you to sign up on the spot.",
            address = "900 Pennsylvania Ave",
            neighborhood = "Bayview",
            phone = "(415) 282-1900",
            website = "https://www.sfmfoodbank.org/find-food",
            hoursText = "Mon–Fri 9am–5pm (phone line)",
            hours = h(WEEK, "09:00", "17:00"),
            requirements = listOf("No ID required"),
            tags = listOf("food", "groceries", "pantry"),
            cost = "Free",
            createdVia = "seed",
            aiTips = "Groceries only help if you can cook or store them — ask for the 'ready to eat' pantry list."
        ),
        Resource(
            name = "Mission Neighborhood Resource Center",
            category = "hygiene",
            alsoOffers = listOf("community", "documents", "shelter", "connect"),
            summary = "Drop-in center with showers, laundry, mail, phones and a coordinated-entry access point.",
            description = "MNRC is one of the most useful single stops in the city: hot showers, laundry, free mail address, phone and computer access, harm reduction supplies, and staff who can put you on the housing queue.",
            address = "165 Capp St",
            neighborhood = "Mission",
            phone = "(415) 869-7977",
            hoursText = "Mon–Fri 7am–2pm; showers sign-up starts 7am",
            hours = h(WEEK, "07:00", "14:00", "drop-in"),
            requirements = listOf("No ID required", "First come, first served — line up early"),
            bring = listOf("Your own towel if you have one"),
            tags = listOf("shower", "laundry", "mail", "mission", "access point"),
            cost = "Free",
            createdVia = "seed",
            aiTips = "Shower list fills within the first 30 minutes. Get there by 6:45am."
        ),
        Resource(
            name = "United Council of Human Services (Mother Brown's)",
            category = "community",
            alsoOffers = listOf("food", "hygiene", "shelter"),
            summary = "24-hour drop-in in the Bayview: meals, showers, seats to rest, and a housing access point.",
            description = "Mother Brown's Dining Room serves meals and the drop-in stays open overnight so you have somewhere indoors. Also an Adult Coordinated Entry Access Point.",
            address = "2111 Jennings St",
            neighborhood = "Bayview",
            phone = "(415) 671-1100",
            hoursText = "Drop-in open 24 hours; meals served 3x daily",
            hours = emptyList(),
            open24 = true,
            requirements = listOf("No ID required"),
            tags = listOf("24-hour", "bayview", "food", "shower", "drop-in"),
            cost = "Free",
            createdVia = "seed"
        ),
        Resource(
            name = "MSC South Shelter & Drop-In",
            category = "shelter",
            alsoOffers = listOf("hygiene", "food", "health"),
            summary = "Large 24-hour shelter and drop-in in SoMa. Showers, meals, medical respite nearby.",
            description = "Run by St. Vincent de Paul Society. One of the main entry points for adult shelter beds in SF. The drop-in area is open around the clock.",
            address = "525 5th St",
            neighborhood = "SoMa",
            phone = "(415) 597-7960",
            hoursText = "24 hours; bed reservations through Access Points or 311",
            hours = emptyList(),
            open24 = true,
            requirements = listOf("No ID required"),
            tags = listOf("24-hour", "soma", "shelter", "beds"),
            cost = "Free",
            createdVia = "seed"
        )
    )

    val SEED_RMP_LOCATIONS = listOf(
        RmpLocation(
            name = "Subway — Tenderloin",
            address = "391 Golden Gate Ave",
            neighborhood = "Tenderloin",
            zip = "94102",
            cuisine = "sandwiches",
            chain = true,
            phone = "(415) 555-0101",
            hoursText = "Daily 7am–10pm",
            hours = h(ALL, "07:00", "22:00"),
            notes = "Accepts EBT cards for hot toasted sandwiches.",
            tips = "Say 'make it hot' or 'toast it' so they process it under the Restaurant Meals Program.",
            confidence = "sfhsa",
            createdVia = "seed"
        ),
        RmpLocation(
            name = "Burger King — SoMa",
            address = "1200 Market St",
            neighborhood = "SoMa",
            zip = "94102",
            cuisine = "burgers",
            chain = true,
            phone = "(415) 555-0102",
            hoursText = "Daily 6am–11pm",
            hours = h(ALL, "06:00", "23:00"),
            notes = "Accepts EBT card for prepared burgers and hot food.",
            confidence = "sfhsa",
            createdVia = "seed"
        ),
        RmpLocation(
            name = "Mission Taqueria",
            address = "2400 Mission St",
            neighborhood = "Mission",
            zip = "94110",
            cuisine = "mexican",
            chain = false,
            phone = "(415) 555-0103",
            hoursText = "Daily 10am–9pm",
            hours = h(ALL, "10:00", "21:00"),
            notes = "Hot burritos and plates. Excellent local spot accepting EBT.",
            tips = "You can get double rice and beans for no extra cost.",
            confidence = "reported",
            createdVia = "seed"
        ),
        RmpLocation(
            name = "Bayview Fried Chicken",
            address = "4800 3rd St",
            neighborhood = "Bayview",
            zip = "94124",
            cuisine = "chicken",
            chain = false,
            phone = "(415) 555-0104",
            hoursText = "Mon–Sat 11am–8pm",
            hours = h(MON_SAT, "11:00", "20:00"),
            notes = "Hot chicken baskets and sides.",
            confidence = "sfhsa",
            createdVia = "seed"
        ),
        RmpLocation(
            name = "Civic Center Cafe",
            address = "100 McAllister St",
            neighborhood = "Civic Center",
            zip = "94102",
            cuisine = "cafe",
            chain = false,
            phone = "(415) 555-0105",
            hoursText = "Mon–Fri 7am–4pm",
            hours = h(WEEK, "07:00", "16:00"),
            notes = "Hot breakfast sandwiches and drip coffee.",
            confidence = "sfhsa",
            createdVia = "seed"
        )
    )

    val SEED_TASKS = listOf(
        Task(
            title = "Get EBT card coded for hot meals",
            notes = "Call CalFresh at (855) 355-5757 and ask them to code the card for the Restaurant Meals Program.",
            kind = "benefit",
            priority = 1
        ),
        Task(
            title = "Visit GLIDE Walk-In Center",
            notes = "Sign up for housing assessment queue and get hygiene kit.",
            kind = "housing",
            priority = 2
        ),
        Task(
            title = "Get a free library card",
            notes = "No fixed address needed. Gives access to computer, internet and warmth.",
            kind = "document",
            priority = 3
        )
    )
}
