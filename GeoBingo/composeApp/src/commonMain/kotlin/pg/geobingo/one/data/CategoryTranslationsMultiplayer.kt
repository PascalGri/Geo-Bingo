package pg.geobingo.one.data

// ─────────────────────────────────────────────────────────────────────────────
//  English translations for the multiplayer category templates (outdoor + indoor).
//  Keyed by the same template id as the German source in Categories.kt. Each value
//  is (English name variants, English description). Applied via
//  Category.localizedTemplate(MP_TEMPLATES_EN) when S.language == EN; missing keys
//  fall back to German. CATEGORY_DESCRIPTIONS_EN is derived from the descriptions
//  here and used for the DB round-trip (categoryDescription()).
// ─────────────────────────────────────────────────────────────────────────────

val MP_TEMPLATES_EN: Map<String, TemplateEn> = mapOf(
    // ── Outdoor: Clothing ──────────────────────────────────────────────────────
    "pants" to (listOf(
        "Yellow trousers", "Red trousers", "Blue trousers", "Green trousers",
        "Checked trousers", "Orange trousers", "White trousers", "Striped trousers",
    ) to "The colour or pattern must be clearly recognisable. Knee to ankle should be visible – no photo from 50 m away."),
    "jacket" to (listOf(
        "Red jacket", "Yellow rain jacket", "Green gilet",
        "Blue down jacket", "White jacket", "Purple coat", "Brown leather jacket",
    ) to "The jacket or coat must be clearly recognisable in the photo. Colour or material must be clearly visible."),
    "hat" to (listOf(
        "Person with a cowboy hat", "Person with a beret", "Person with a straw hat",
        "Person with a woolly hat", "Person with a backwards cap", "Person with a top hat",
    ) to "Both the person and the headwear must be in the photo. The type of hat must be clearly recognisable."),
    "sneaker" to (listOf(
        "Bright red sneakers", "White Air Force Ones", "Colourful shoes",
        "Neon-yellow running shoes", "Shiny boots", "Patterned shoes",
    ) to "The shoes must be the focus of the photo and the target feature (colour/pattern/model) clearly recognisable."),

    // ── Outdoor: Vehicles ──────────────────────────────────────────────────────
    "car" to (listOf(
        "Red Porsche", "White Tesla", "Black BMW",
        "Silver Mercedes", "Blue Ferrari", "Yellow Fiat 500",
        "Green Mini Cooper", "Pink Smart",
    ) to "The vehicle must be fully or at least half visible. Make and colour must be clearly recognisable."),
    "vehicle" to (listOf(
        "E-scooter", "Motorbike", "Delivery van",
        "Tram", "Classic car", "Ambulance", "Police car",
    ) to "The target vehicle must be clearly recognisable in the photo. No toy versions."),
    "bicycle" to (listOf(
        "Parked bike with flower decoration", "Fixie against a lamppost",
        "Bike with a trailer", "Retro bike", "Tandem bike",
        "Bike with a full basket",
    ) to "The bike and the target feature (e.g. decoration, trailer) must both be clearly visible."),

    // ── Outdoor: Animals ───────────────────────────────────────────────────────
    "animal" to (listOf(
        "Dog on a leash", "Cat on a windowsill", "Pigeon on the ground",
        "Duck by the water", "Squirrel in the park", "Seagull in flight",
    ) to "The animal and its situation must be recognisable. The animal must be alive and real – no picture or toy."),
    "dog_breed" to (listOf(
        "Golden Retriever", "Little pug", "Dalmatian",
        "Giant Schnauzer", "Bulldog", "Greyhound",
    ) to "The dog's breed must be recognisable. For mixed breeds the main breed counts – the vote decides."),
    "bird" to (listOf(
        "Sparrow on a table", "Crow on a bin", "Swan on the water",
        "Magpie on a roof", "Parrot on a shoulder", "Robin on a branch",
    ) to "The bird species and location must be recognisable in the photo. Close-up shots preferred."),

    // ── Outdoor: Nature ────────────────────────────────────────────────────────
    "nature" to (listOf(
        "Blooming flower", "Moss on a stone", "Mushroom",
        "Ivy on a wall", "Cactus", "Bamboo", "Wildflowers",
    ) to "The natural object must be clearly recognisable and outdoors. Potted plants in shops don't count."),
    "tree" to (listOf(
        "Cherry tree in blossom", "Huge old oak", "Weeping willow by the water",
        "Palm tree (real or fake)", "Tree with colourful leaves", "Bonsai in a shop window",
    ) to "The tree or the target feature must be clearly recognisable. Small shrubs don't count as trees."),
    "water" to (listOf(
        "Drinking fountain", "Puddle reflection", "Stream",
        "Canal", "Pond with ducks", "Fountain",
    ) to "The water feature must be clearly in focus. For reflections, the reflection must be recognisable."),
    "sky" to (listOf(
        "Dramatic storm cloud", "Heart-shaped cloud", "Crossing contrails",
        "Patch of rainbow", "Full moon visible by day", "Sunbeam through clouds",
    ) to "The sky phenomenon must be clearly recognisable. Edited or filtered photos are invalid."),

    // ── Outdoor: Food & Drinks ─────────────────────────────────────────────────
    "food" to (listOf(
        "Eating ice cream", "Pretzel in hand", "Kebab stand",
        "Fruit stall", "Coffee to go", "Eating a waffle",
    ) to "The food and the person or place must be recognisable. The target food must be the focal point."),
    "food_truck" to (listOf(
        "Burger truck", "Taco stand", "Crêpe van",
        "Currywurst stand", "Bubble tea kiosk", "Sushi roller",
    ) to "The truck or stand must be recognisable. The type of food on offer must be clear from the photo or a sign."),
    "cafe" to (listOf(
        "Outdoor café with sun umbrellas", "Café with a chalkboard menu",
        "Café with a cat inside", "Hipster coffee shop", "Ice cream parlour with a queue",
    ) to "The café and the target feature (e.g. sun umbrellas, chalkboard) must be recognisable in the photo."),
    "street_food" to (listOf(
        "Someone eating fries", "Breakfast on a park bench",
        "Picnic group on the grass", "Person with a huge sandwich",
        "Child with an ice lolly",
    ) to "The person and the food must be clearly recognisable. The scene must take place outdoors."),

    // ── Outdoor: Architecture ──────────────────────────────────────────────────
    "building" to (listOf(
        "Old half-timbered house", "Modern glass building", "Old church",
        "Castle or tower", "Town hall", "Historic city gate",
    ) to "The type of building must be clearly recognisable. At least the façade must be fully visible."),
    "door" to (listOf(
        "Bright red front door", "Old wooden door with a brass handle",
        "Turquoise front door", "Door with climbing plants",
        "Door with a colourful knocker", "Double door with stained glass",
    ) to "The door must be clearly recognisable. The target feature (colour, material, decoration) must be clearly visible."),
    "window" to (listOf(
        "Window with flowers", "Window with a cat", "Stained-glass window",
        "Shop window with a mannequin", "Window with fairy lights",
    ) to "The window and the target feature must both be clearly recognisable in the photo."),
    "balcony" to (listOf(
        "Balcony full of plants", "Balcony with a bike on it",
        "Balcony with a washing line", "Balcony with a mini pool",
        "Balcony with beehives", "Balcony with a flag",
    ) to "The balcony and its special feature must both be recognisable. Only balconies on real residential buildings."),
    "arch" to (listOf(
        "Stone archway", "Modern metal tunnel",
        "Bridge arch over water", "Arcade in the old town",
        "Visible vaulted cellar entrance", "Round-arch window",
    ) to "The arch shape must be clearly recognisable. Both sides of the arch must be in the photo."),

    // ── Outdoor: Street Art ────────────────────────────────────────────────────
    "street_art" to (listOf(
        "Graffiti on a wall", "Mural", "Street painting",
        "Mosaic", "Art installation", "Painted utility box",
    ) to "The art form must be recognisable and in a public place. Advertising posters don't count."),
    "mural" to (listOf(
        "Huge façade painting", "Political wall painting",
        "Abstract colour-explosion mural", "Animal portrait on a wall",
        "Optical-illusion mural", "Painted retro advertising sign",
    ) to "The mural must be large-format and clearly artistic. The photo must show the whole work, not just a detail."),
    "sculpture" to (listOf(
        "Modern sculpture in the park", "Classical statue by a fountain",
        "Interactive art installation", "Larger-than-life animal sculpture",
        "Abstract metal figure", "Stone face carved into a wall",
    ) to "The sculpture must be clearly recognisable as an art object and be fully or almost fully visible."),
    "pattern" to (listOf(
        "Chequerboard pattern on the ground", "Symmetrical tile pattern",
        "Herringbone paving", "Circular mosaic area",
        "Striped pattern on a façade", "Geometric grid",
    ) to "The pattern must be clearly recognisable and repeating. The photo must show at least 4 repetitions."),

    // ── Outdoor: Signs ─────────────────────────────────────────────────────────
    "sign" to (listOf(
        "Funny sign", "Signpost", "House number 13",
        "Prohibition sign", "Construction sign", "Historic street sign",
    ) to "The sign and its text must be legible. Digital display boards count."),
    "funny_sign" to (listOf(
        "Sign with a spelling mistake", "Absurdly long signpost",
        "Contradictory prohibition sign", "Sign with a funny message",
        "Hand-painted sign", "Sign that contradicts itself",
    ) to "The funny aspect must be clearly recognisable and legible. The vote decides whether it's funny enough."),
    "number" to (listOf(
        "House number 42", "Bus destination with the number 7", "12:00 on a clock",
        "The number 100 somewhere", "Street name with a number", "Parking space number",
    ) to "The target number must be clearly legible. It must be officially displayed – no self-written numbers."),

    // ── Outdoor: Light & Reflection ────────────────────────────────────────────
    "shadow" to (listOf(
        "Long human shadow", "Grid shadow", "Tree shadow",
        "Animal shadow", "Double shadow", "Shadow in a shape",
    ) to "The shadow must be clearly recognisable and show the target feature. The light source must not be in the photo."),
    "reflection" to (listOf(
        "Reflection in a puddle", "Reflection in a shop window",
        "Reflection in sunglasses", "Reflection in a car mirror",
        "Reflection in a water fountain", "Double reflection in a glass building",
    ) to "The reflection and the reflecting surface must both be recognisable. Edited photos are invalid."),
    "light" to (listOf(
        "Sunrays through treetops", "Shadow kaleidoscope on the ground",
        "Fairy lights on a tree", "Neon sign", "Lantern with a shadow",
        "Rainbow in a sprinkler",
    ) to "The light phenomenon must be clearly recognisable and taken outdoors or in a public place."),

    // ── Outdoor: Sports ────────────────────────────────────────────────────────
    "sport" to (listOf(
        "Jogger", "Cyclist with a helmet", "Skater",
        "Person with a yoga mat", "Climber", "Inline skater",
    ) to "The person and the sport must be recognisable. The person must be actively moving or clearly dressed as an athlete."),
    "sport_field" to (listOf(
        "Basketball hoop in the park", "Beach volleyball net",
        "Outdoor table-tennis table", "Boules players", "Skate park",
        "Neighbourhood football cage",
    ) to "The sports facility or equipment must be clearly recognisable. People playing are a plus but not required."),

    // ── Outdoor: Technology ────────────────────────────────────────────────────
    "tech" to (listOf(
        "EV charging point", "Solar panels on a roof", "Surveillance camera",
        "Photo booth", "ATM", "Parcel locker",
    ) to "The device must be clearly recognisable. It must be in a public place and in operation."),
    "smart_city" to (listOf(
        "Smart streetlight with a sensor", "Wi-Fi hotspot pillar",
        "E-scooter parking station", "Bike-sharing station",
        "Digital info board", "Robot delivery vehicle",
    ) to "The smart street furniture must be clearly recognisable. A visible logo or sign as proof is ideal."),
    "construction" to (listOf(
        "Crane over the skyline", "Freshly painted wall",
        "Construction site with barrier tape", "Scaffolding on a façade",
        "Excavator on the street", "New building under construction",
    ) to "The construction site or element must be active and clearly recognisable. Bare construction fences alone aren't enough."),

    // ── Outdoor: Urban Furniture ───────────────────────────────────────────────
    "bench" to (listOf(
        "Park bench with a person", "Empty park bench", "Bench by the water",
        "Swing in the park", "Deckchair", "Stone bench",
    ) to "The seat must be fully in the photo. The target feature (person, location) must be recognisable."),
    "stairs" to (listOf(
        "Outdoor staircase", "Spiral staircase", "Fire escape",
        "Escalator", "Historic stone staircase", "Bridge with steps",
    ) to "At least 5 steps must be visible. The target type must be clearly recognisable."),
    "manhole" to (listOf(
        "Decorative manhole cover", "Painted drain cover",
        "Manhole cover with a city logo", "Rusty iron plate in the paving",
        "Grate cover with a pattern",
    ) to "The cover must be fully visible and the pattern or design clearly recognisable."),
    "mailbox" to (listOf(
        "Red postbox", "Old wall-mounted mailbox",
        "Stuffed mailbox", "Mailbox with a funny name",
        "Little book-swap box", "Parcel safe at a front door",
    ) to "The mailbox must be fully visible and the target feature (colour, condition) recognisable."),
    "clock" to (listOf(
        "Public tower clock", "Sundial in the park",
        "Watch shop window", "Huge wall clock on a building",
        "Station clock", "Digital city time display",
    ) to "The clock and its numerals or the time must be recognisable. Wristwatches don't count."),
    "lamp" to (listOf(
        "Old gas lantern", "Modern design streetlight",
        "Fairy lights between houses", "Neon sign",
        "LED art installation", "Hanging paper lantern",
    ) to "The light source and its special feature must be clearly recognisable. Ordinary light bulbs in shops don't count."),

    // ── Outdoor: People ────────────────────────────────────────────────────────
    "people" to (listOf(
        "Group of laughing people", "Queue", "Schoolchildren together",
        "Tourist group taking a photo", "Sports group", "Couple holding hands",
    ) to "At least 2 people must be recognisable in the photo. Mind consent – no sneaky close-ups."),
    "person_accessory" to (listOf(
        "Person with a hat", "Person with sunglasses", "Person with headphones",
        "Person with a backpack", "Person with an umbrella", "Person with a wheeled suitcase",
    ) to "The person and the accessory must be clearly recognisable. Photograph with the consent of the person shown."),
    "musician" to (listOf(
        "Street guitarist", "Accordion player", "Drummer with buckets",
        "Violinist", "Saxophonist on a corner", "Street choir",
    ) to "The musician and the instrument must be recognisable. The person must be actively playing or clearly identifiable as a musician."),
    "funny_pose" to (listOf(
        "Person doing a handstand", "Someone jumping and laughing",
        "Person taking a selfie in a funny pose", "Person dancing alone",
        "Person asleep sitting on a bench", "Someone carrying a huge object",
    ) to "The funny situation must be clearly recognisable. Best photographed with the person's consent."),
    "tourist" to (listOf(
        "Tourist photographing a building", "Tour group with audio guides",
        "Person with a huge map", "Selfie in front of a landmark",
        "Tour guide with an umbrella group", "Person in a typical souvenir outfit",
    ) to "The typically touristy behaviour must be clearly recognisable. Photos with the consent of those shown."),

    // ── Outdoor: Shops ─────────────────────────────────────────────────────────
    "shop" to (listOf(
        "Bakery", "Flower shop", "Hairdresser",
        "Bookshop", "Cheese shop", "Wine merchant", "Organic store",
    ) to "The shop and its type must be clearly recognisable – either by a sign or by the shop window."),
    "market" to (listOf(
        "Farmers' market stall", "Flea market table", "Flower market",
        "Colourful fruit market", "Antiques stall",
        "Second-hand clothes stall",
    ) to "The stall and its goods must be recognisable. Supermarket interiors don't count."),

    // ── Outdoor: Colors ────────────────────────────────────────────────────────
    "color_object" to (listOf(
        "Something completely red", "Something completely blue", "Something completely yellow",
        "Something completely orange", "Something completely pink", "Something completely white",
    ) to "The object must be at least 80% in the target colour. Mixed colours don't count."),
    "colorful_scene" to (listOf(
        "Rainbow flag or decoration", "5 different colours in one photo",
        "Brightly painted house", "Colourful bikes side by side",
        "Colourful mosaic", "Confetti on the ground",
    ) to "The photo must clearly show the stated number of colours or the described colourful object."),

    // ── Outdoor: Geometry ──────────────────────────────────────────────────────
    "round" to (listOf(
        "Round road sign", "Round mirror", "Round table",
        "Round clock", "Round fountain", "Round window",
    ) to "The object must be clearly round – no oval or only slightly rounded objects. Fully visible."),
    "triangle" to (listOf(
        "Triangular roof", "Warning signs (triangle)", "Triangular window arrangement",
        "Pyramid-shaped architecture", "Triangle pattern on the ground",
    ) to "The triangle shape must be clearly recognisable. The photo must show the whole triangle or pattern."),
    "symmetry" to (listOf(
        "Perfectly symmetrical façade", "Mirror-image bridge in the water",
        "Two identical trees left and right", "Symmetrical archway",
        "Mirrored row of windows", "Left-right double staircase",
    ) to "The axis of symmetry must be recognisable and both halves must be fully in the photo."),

    // ── Outdoor: Culture ───────────────────────────────────────────────────────
    "music" to (listOf(
        "Street musician", "Concert poster", "Headphones on a person",
        "Speaker outdoors", "Instrument in a shop window", "Visible sheet music",
    ) to "The music element must be clearly recognisable. For posters, the text must be legible."),
    "culture" to (listOf(
        "Theatre poster", "Cinema entrance", "Gallery window",
        "Dance performance in the park", "Craft flea market",
        "Book-swap cabinet",
    ) to "The cultural element must be clearly recognisable. Active events or recognisable places."),
    "vintage" to (listOf(
        "Old telephone receiver", "Retro shop window", "Vintage advertising sign",
        "Old pharmacy lettering", "Historic shop name", "Antique street lantern",
    ) to "The object must be recognisably old, historic or retro-styled. Modern imitations only count with a clear retro aesthetic."),

    // ── Outdoor: Curious ───────────────────────────────────────────────────────
    "weird" to (listOf(
        "Something completely out of place", "Unexpectedly funny detail",
        "A sign that makes no sense", "Very unusual colour combination",
        "An object standing the wrong way round", "Animal in an unusual place",
    ) to "The curious thing must be recognisable to everyone in the photo. The vote decides whether it's really \"weird\" enough."),
    "miniature" to (listOf(
        "Tiny street-art detail", "Mini figure in a pavement crack",
        "Small hidden sign", "Micro-art on a lamppost",
        "Very small graffiti tag", "Tiny door built into a wall",
    ) to "The small detail must be clearly recognisable and in focus. A finger or coin for scale is a plus."),
    "texture" to (listOf(
        "Rough brick wall", "Smooth marble floor", "Weathered wood",
        "Rusty metal", "Cracked asphalt", "Coarse natural stone",
    ) to "The texture must fill the frame and be recognisable in detail. At least 50% of the photo."),
    "surface" to (listOf(
        "Mirrored glass façade", "Textured plaster", "Grained leather",
        "Corrugated iron", "Sandblasted concrete", "Polished granite",
    ) to "The surface and its material must be clearly recognisable and in focus."),
    "contrast" to (listOf(
        "Old next to new", "Big next to small", "Nature next to concrete",
        "Light next to dark", "Round next to angular", "Colourful next to grey",
    ) to "Both elements of the contrast must be recognisable in the same photo."),

    // ── Outdoor: Nature (weather/seasons) ──────────────────────────────────────
    "weather" to (listOf(
        "Raindrops on glass", "Fog in the distance", "Wind-blown tree",
        "Puddle with raindrops", "Frost on a leaf", "Slush on the pavement",
    ) to "The weather phenomenon must be clearly recognisable. Natural shots, no filters."),
    "seasonal" to (listOf(
        "Autumn leaves on the ground", "Spring blossom on a tree", "Summery shade",
        "Wintry scene", "Seasonal decoration", "Seasonal market stall",
    ) to "The season must be clearly recognisable from the photo."),

    // ── Outdoor: Transport ─────────────────────────────────────────────────────
    "public_transport" to (listOf(
        "Bus at a stop", "Underground entrance", "Timetable at a stop",
        "Tram in motion", "Ticket machine", "People waiting on a platform",
    ) to "The public transport or infrastructure must be recognisable."),
    "parking" to (listOf(
        "Full car park", "Car park entrance", "Parking meter on the street",
        "Disabled parking space", "Bicycle parking", "Resident parking permit",
    ) to "The parking situation must be clearly recognisable. A sign or marking must be visible."),

    // ── Outdoor: Food (extended) ───────────────────────────────────────────────
    "bakery" to (listOf(
        "Bakery display", "Fresh bread on a shelf", "Cake in a display case",
        "Croissant on a plate", "Bag of bread rolls", "Patisserie window",
    ) to "The baked goods or bakery must be recognisable. Home-baked goods don't count."),
    "drink" to (listOf(
        "Coffee in hand", "Smoothie in a cup", "Beer garden scene",
        "Juice at a kiosk", "Water bottle while exercising", "Cocktail on a terrace",
    ) to "The drink and its context must be recognisable."),

    // ── Indoor templates ───────────────────────────────────────────────────────
    "in_chair" to (listOf(
        "Unusual chair", "Red armchair", "Stackable chair",
        "Rocking chair", "Swivel chair", "Stool at a counter",
    ) to "The seat and its type must be clearly recognisable."),
    "in_lamp" to (listOf(
        "Designer lamp", "Chandelier", "Floor lamp in a corner",
        "Desk lamp", "Fairy lights on a wall", "Pendant light",
    ) to "The lamp and its design must be recognisable. Ordinary ceiling lights are too easy."),
    "in_plant" to (listOf(
        "Monstera", "Cactus on a windowsill", "Hanging plant",
        "Succulent in a small pot", "Orchid", "Fern in the bathroom",
    ) to "The plant and its location must be recognisable. Must be a real plant."),
    "in_book" to (listOf(
        "Stack of books", "Open book", "Bookshelf full of books",
        "Comic or graphic novel", "Cookbook in the kitchen", "Children's book with a colourful cover",
    ) to "The book or books must be recognisable. The title or cover should be visible."),
    "in_mirror" to (listOf(
        "Large wall mirror", "Bathroom mirror", "Hand mirror",
        "Reflective surface", "Selfie in a mirror", "Framed mirror",
    ) to "The mirror and a reflection must be recognisable."),
    "in_cup" to (listOf(
        "Coffee cup", "Colourful mug", "Teacup with a saucer",
        "Travel mug", "Espresso cup", "Mug with a funny slogan",
    ) to "The cup and its feature must be recognisable."),
    "in_pattern" to (listOf(
        "Tile pattern in the bathroom", "Wallpaper pattern", "Carpet pattern",
        "Cushion pattern", "Curtain pattern", "Patterned crockery",
    ) to "The pattern must be clearly recognisable and repeating."),
    "in_kitchen" to (listOf(
        "Open fridge", "Spice collection", "Cutting board with a knife",
        "Food processor", "Fruit bowl", "Pot collection",
    ) to "The kitchen object must be recognisable and in a kitchen context."),
    "in_tech" to (listOf(
        "Laptop on a table", "TV on a wall", "Games console",
        "Smartwatch", "Tablet on a sofa", "Router with blinking lights",
    ) to "The device must be switched on or clearly recognisable."),
    "in_textile" to (listOf(
        "Cosy blanket on a sofa", "Stack of towels", "Patterned tablecloth",
        "Knitted jumper", "Velvet cushion", "Patchwork rug",
    ) to "The textile and its texture must be recognisable."),
    "in_art" to (listOf(
        "Picture on the wall", "Poster", "Photo in a frame",
        "Self-painted picture", "Postcard collage", "Wall decoration",
    ) to "The artwork and its subject must be recognisable."),
    "in_toy" to (listOf(
        "Soft toy", "Board game box", "Partly solved puzzle",
        "LEGO model", "Doll or action figure", "Card game on a table",
    ) to "The toy must be clearly recognisable."),
    "in_shoe" to (listOf(
        "Shoe rack", "Single shoe on the floor", "Slippers",
        "Trainers by the entrance", "Boots in the hallway", "Shoes in a row",
    ) to "The shoes and their situation must be recognisable."),
    "in_door" to (listOf(
        "Open room door", "Cupboard door with a handle", "Bathroom door",
        "Cellar door", "Glass door", "Door with a poster",
    ) to "The door and its special feature must be recognisable."),
    "in_color_red" to (listOf(
        "Red cushion", "Red packaging", "Red book",
        "Red apple", "Red mug", "Red item of clothing",
    ) to "The object must be predominantly red and clearly recognisable."),
    "in_color_blue" to (listOf(
        "Blue towel", "Blue bottle", "Blue bowl",
        "Blue T-shirt", "Blue ballpoint pen", "Blue toothbrush",
    ) to "The object must be predominantly blue and clearly recognisable."),
    "in_glass" to (listOf(
        "Wine glass", "Glass vase", "Glass bowl",
        "Patterned drinking glass", "Glass bottle", "Glass decoration",
    ) to "The glass object must be recognisable and in focus."),
    "in_wood" to (listOf(
        "Wooden table", "Wooden cutting board", "Wooden shelf",
        "Wooden frame", "Wooden spoon", "Wooden figure",
    ) to "The wooden object and its grain must be recognisable."),
    "in_candle" to (listOf(
        "Scented candle", "Tealight in a holder", "Candlestick",
        "Advent wreath candle", "LED candle", "Candle on a table",
    ) to "The candle and its context must be recognisable."),
    "in_bottle" to (listOf(
        "Water bottle", "Wine bottle", "Perfume bottle",
        "Shampoo bottle", "Spice mill", "Oil bottle in the kitchen",
    ) to "The bottle and its contents or label must be recognisable."),
)

/** template id → English description, derived from [MP_TEMPLATES_EN] for the DB round-trip. */
val CATEGORY_DESCRIPTIONS_EN: Map<String, String> = MP_TEMPLATES_EN.mapValues { it.value.second }
