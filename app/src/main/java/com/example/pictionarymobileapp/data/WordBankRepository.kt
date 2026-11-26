package com.example.pictionarymobileapp.data

// Word bank repository for themes, word selection, and guess validation
object WordBankRepository {

    // Themes
    val themes: List<String> = listOf("Animals", "Locations", "Objects", "Halloween", "Christmas")

    // Map of themes to lists of words
    private val wordBank: Map<String, List<String>> = mapOf(
        "Animals" to listOf(
            "Cat", "Dog", "Elephant", "Lizard", "Shark",
            "Tiger", "Penguin", "Dolphin", "Panda", "Giraffe",
            "Lion", "Bear", "Monkey", "Rabbit", "Horse",
            "Cow", "Pig", "Sheep", "Goat", "Chicken",
            "Duck", "Goose", "Turkey", "Owl", "Eagle",
            "Parrot", "Flamingo", "Peacock", "Swan", "Crow",
            "Squirrel", "Deer", "Moose", "Fox", "Wolf",
            "Raccoon", "Skunk", "Beaver", "Otter", "Badger",
            "Hedgehog", "Bat", "Mouse", "Rat", "Hamster",
            "Guinea Pig", "Ferret", "Kangaroo", "Koala", "Sloth",
            "Zebra", "Rhino", "Hippo", "Camel", "Donkey",
            "Crocodile", "Alligator", "Turtle", "Snake", "Frog",
            "Toad", "Salamander", "Chameleon", "Gecko", "Iguana",
            "Spider", "Scorpion", "Ant", "Bee", "Butterfly",
            "Ladybug", "Dragonfly", "Grasshopper", "Crab", "Lobster",
            "Octopus", "Squid", "Jellyfish", "Starfish", "Seahorse",
            "Whale", "Seal", "Walrus", "Sea Lion", "Stingray",
            "Eel", "Pufferfish", "Goldfish", "Salmon", "Tuna",
            "Piranha", "Peacock", "Pelican", "Woodpecker", "Hummingbird"
        ),
        "Locations" to listOf(
            "Beach", "Airport", "Hospital", "Library", "Park",
            "Museum", "Restaurant", "School", "Store", "Zoo",
            "Farm", "Forest", "Desert", "Mountain", "Island",
            "Lake", "River", "Waterfall", "Cave", "Volcano",
            "Castle", "Palace", "Church", "Temple", "Mosque",
            "Hotel", "Motel", "Mall", "Supermarket", "Bakery",
            "Bank", "Post Office", "Fire Station", "Police Station", "Prison",
            "Gas Station", "Car Wash", "Barbershop", "Hair Salon", "Gym",
            "Swimming Pool", "Playground", "Stadium", "Basketball Court", "Golf Course",
            "Bowling Alley", "Movie Theater", "Amusement Park", "Water Park", "Circus",
            "Camping Site", "Campground", "Garden", "Greenhouse", "Barn",
            "Ranch", "Factory", "Warehouse", "Construction Site", "Office",
            "Apartment", "House", "Mansion", "Treehouse", "Cabin",
            "Garage", "Basement", "Attic", "Kitchen", "Bathroom",
            "Bedroom", "Living Room", "Classroom", "Cafeteria", "Train Station",
            "Bus Stop", "Subway Station", "Harbor", "Port", "Bridge",
            "Tunnel", "Lighthouse", "Pier", "Dock", "Parking Lot",
            "Street", "Highway", "Crosswalk", "Farmers Market", "Flea Market"
        ),
        "Objects" to listOf(
            "Pencil", "Pen", "Keyboard", "Laptop", "Computer",
            "Hair Dryer", "Toothbrush", "Pillow", "Jacket", "Painting",
            "Book", "Notebook", "Eraser", "Ruler", "Scissors",
            "Backpack", "Suitcase", "Wallet", "Purse", "Umbrella",
            "Watch", "Clock", "Glasses", "Bracelet", "Hat",
            "Shoes", "Socks", "Gloves", "Scarf", "Belt",
            "Phone", "Tablet", "Camera", "Television", "Remote",
            "Headphones", "Speaker", "Microphone", "Flashlight", "Battery",
            "Charger", "Light Bulb", "Lamp", "Fan", "Mirror",
            "Comb", "Brush", "Soap", "Towel", "Razor",
            "Cup", "Mug", "Glass", "Plate", "Bowl",
            "Fork", "Spoon", "Knife", "Pan", "Pot",
            "Kettle", "Toaster", "Blender", "Microwave", "Refrigerator",
            "Oven", "Dishwasher", "Broom", "Mop", "Vacuum",
            "Chair", "Table", "Desk", "Couch", "Bed",
            "Drawer", "Cabinet", "Bookshelf", "Door", "Stuffed Animal",
            "Key", "Lock", "Hammer", "Screwdriver", "Wrench",
            "Saw", "Drill", "Ladder", "Rope", "Bucket"
        ),
        "Halloween" to listOf(
            "Pumpkin", "Pumpkin Patch", "Jack-O-Lantern", "Ghost", "Monster",
            "Spider", "Werewolf", "Michael Myers", "Jack Skeleton", "Freddy Fazbear",
            "Witch", "Vampire", "Zombie", "Skeleton", "Mummy",
            "Bat", "Black Cat", "Owl", "Crow", "Rat",
            "Frankenstein", "Dracula", "Grim Reaper", "Demon", "Devil",
            "Angel", "Goblin", "Troll", "Witch Hat", "Witch Broom",
            "Cauldron", "Magic Wand", "Spell Book", "Crystal Ball", "Potion",
            "Haunted House", "Graveyard", "Coffin", "Tombstone", "Freddy Krueger",
            "Cemetery", "Spiderweb", "Skull", "Bones", "Scarecrow",
            "Candy", "Candy Corn", "Lollipop", "Chocolate", "Caramel Apple",
            "Trick or Treat", "Ghost Face", "Halloween Mask", "Costume", "Cloak",
            "Fangs", "Bloody Knife", "Axe", "Chainsaw", "Machete",
            "Candle", "Lantern", "Black Widow", "Eyeball", "Severed Hand",
            "Curse", "Clown", "Scary Mask", "Monster Truck", "Fog",
            "Full Moon", "Lightning", "Chucky", "Haunted Mirror", "Creaky Door",
            "Raven", "Mushroom", "Voodoo Doll", "Hex", "Flying Dutchman",
            "Vampire Bat", "Horror Movie", "Blood", "Broomstick", "Trick",
            "Treat", "Apple Bobbing", "Ritual", "Spooky Tree", "Creepy House"
        ),
        "Christmas" to listOf(
            "Santa", "Mrs Claus", "Elf", "Toys", "Present",
            "Sled", "Nice List", "Naughty List", "Jingle Bells", "Egg Nog",
            "Christmas Tree", "Star", "Ornament", "Tinsel", "Garland",
            "Wreath", "Stocking", "Candy Cane", "Gingerbread Man", "Gingerbread House",
            "Snowman", "Snowflake", "Snowball", "Icicle", "Snow Globe",
            "Reindeer", "Rudolph", "Polar Bear", "Penguin", "Polar Express",
            "Sleigh", "Christmas Spirit", "Santa Hat", "Sleigh Ride", "Nightmare Before Christmas",
            "Chimney", "Fireplace", "North Pole", "Workshop", "Toy Factory",
            "Wrapping Paper", "Gift Box", "Ribbon", "Bow", "Gift Tag",
            "Christmas Card", "Christmas Lights", "White Elephant", "Candle", "Bell",
            "Mistletoe", "Holly", "Poinsettia", "Pine Tree", "Pinecone",
            "Angel", "Nutcracker", "Jack Frost", "Christmas Village", "Train Set",
            "Hot Chocolate", "Marshmallow", "Gingerbread Cookie", "Sugar Cookie", "Fruitcake",
            "Turkey", "Ham", "Christmas Pudding", "Apple Pie", "Peppermint",
            "Milk and Cookies", "Krampus", "Christmas Dinner", "Frosty", "Carolers",
            "Christmas Carol", "Church", "Secret Santa", "Snowy Cabin", "Ice Skates",
            "Ice Rink", "Winter Coat", "Mittens", "Scarf", "Earmuffs",
            "Christmas Sweater", "Ugly Sweater", "Chestnut", "Advent Calendar", "Christmas Eve"
        )
    )

    // Retrieves random word options for a given theme
    fun getRandomWordChoices(theme: String, count: Int = 3): List<String> {
        val words = wordBank[theme] ?: wordBank["Animals"]!!
        return words.shuffled().take(count.coerceAtMost(words.size))
    }

    // Validates whether a guess matches the target word
    fun isCorrectGuess(targetWord: String?, guess: String): Boolean {
        if (targetWord.isNullOrBlank() || guess.isBlank()) return false
        return targetWord.trim().equals(guess.trim(), ignoreCase = true)
    }
}
