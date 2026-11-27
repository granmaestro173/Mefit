"""Builds app/src/main/assets/recipes.json.

Nutrition per 100 g comes from USDA FoodData Central SR Legacy (public domain), taken from the
usda_sr_legacy rows of the tempo-food-db npm package. Recipes are original to MeFit.
"""
import json, sys

# key: (USDA SR Legacy description, kcal, protein, carbs, fat) per 100 g
USDA = {
 "egg": ("Egg, whole, raw, fresh", 143, 12.6, 0.7, 9.5),
 "egg_white": ("Egg, white, raw, fresh", 52, 10.9, 0.7, 0.2),
 "oats": ("Cereals, oats, regular and quick, not fortified, dry", 379, 13.2, 67.7, 6.5),
 "milk": ("Milk, fluid, 1% fat, without added vitamin A and vitamin D", 42, 3.4, 5.0, 1.0),
 "greek_yogurt": ("Yogurt, Greek, plain, nonfat", 59, 10.2, 3.6, 0.4),
 "banana": ("Bananas, raw", 89, 1.1, 22.8, 0.3),
 "blueberries": ("Blueberries, raw", 57, 0.7, 14.5, 0.3),
 "strawberries": ("Strawberries, raw", 32, 0.7, 7.7, 0.3),
 "apple": ("Apples, raw, with skin", 52, 0.3, 13.8, 0.2),
 "peanut_butter": ("Peanut butter, smooth style, without salt", 598, 22.2, 22.3, 51.4),
 "chicken": ("Chicken, broiler or fryers, breast, skinless, boneless, meat only, raw", 120, 22.5, 0.0, 2.6),
 "beef90": ("Beef, ground, 90% lean meat / 10% fat, raw", 176, 20.0, 0.0, 10.0),
 "turkey93": ("Turkey, ground, 93% lean, 7% fat, raw", 150, 18.7, 0.0, 8.3),
 "salmon": ("Fish, salmon, Atlantic, farmed, raw", 208, 20.4, 0.0, 13.4),
 "tuna": ("Fish, tuna, light, canned in water, drained solids", 86, 19.4, 0.0, 1.0),
 "sardines": ("Fish, sardine, Atlantic, canned in oil, drained solids with bone", 208, 24.6, 0.0, 11.5),
 "rice_white": ("Rice, white, long-grain, cooked", 130, 2.7, 28.2, 0.3),
 "rice_brown": ("Rice, brown, long-grain, cooked", 123, 2.7, 25.6, 1.0),
 "pasta": ("Pasta, cooked, without added salt", 158, 5.8, 30.9, 0.9),
 "potato": ("Potatoes, boiled, cooked without skin, flesh, without salt", 86, 1.7, 20.0, 0.1),
 "sweet_potato": ("Sweet potato, raw, unprepared", 86, 1.6, 20.1, 0.1),
 "cassava": ("Cassava, raw", 160, 1.4, 38.1, 0.3),
 "black_beans": ("Beans, black, mature seeds, cooked, boiled", 132, 8.9, 23.7, 0.5),
 "lentils": ("Lentils, mature seeds, cooked, boiled, without salt", 116, 9.0, 20.1, 0.4),
 "chickpeas": ("Chickpeas (garbanzo beans, bengal gram), mature seeds, cooked, boiled", 164, 8.9, 27.4, 2.6),
 "tofu": ("Tofu, firm, prepared with calcium sulfate and magnesium chloride (nigari)", 78, 9.0, 2.9, 4.2),
 "cottage": ("Cheese, cottage, lowfat, 2% milkfat", 81, 10.5, 4.8, 2.3),
 "ricotta": ("Cheese, ricotta, part skim milk", 138, 11.4, 5.1, 7.9),
 "mozzarella": ("Cheese, mozzarella, part skim milk", 254, 24.3, 2.8, 15.9),
 "feta": ("Cheese, feta", 265, 14.2, 3.9, 21.5),
 "parmesan": ("Cheese, parmesan, grated", 420, 28.4, 13.9, 27.8),
 "whey": ("Beverages, Protein powder whey based", 352, 78.1, 6.3, 1.6),
 "bread_ww": ("Bread, whole-wheat", 252, 12.5, 42.7, 3.5),
 "tortilla": ("Tortillas, ready-to-bake or -fry, flour, shelf stable", 297, 8.0, 49.3, 7.6),
 "rice_cake": ("Snacks, rice cakes, brown rice, multigrain", 387, 8.5, 80.1, 3.5),
 "olive_oil": ("Oil, olive, salad or cooking", 884, 0.0, 0.0, 100.0),
 "avocado": ("Avocados, raw", 160, 2.0, 8.5, 14.7),
 "spinach": ("Spinach, raw", 23, 2.9, 3.6, 0.4),
 "broccoli": ("Broccoli, raw", 34, 2.8, 6.6, 0.4),
 "red_pepper": ("Peppers, sweet, red, raw", 26, 1.0, 6.0, 0.3),
 "tomato": ("Tomatoes, red, ripe, cooked", 18, 1.0, 4.0, 0.1),
 "tomato_canned": ("Tomatoes, red, ripe, canned, packed in tomato juice", 16, 0.8, 3.5, 0.3),
 "onion": ("Onions, raw", 40, 1.1, 9.3, 0.1),
 "garlic": ("Garlic, raw", 149, 6.4, 33.1, 0.5),
 "carrot": ("Carrots, raw", 41, 0.9, 9.6, 0.2),
 "cucumber": ("Cucumber, with peel, raw", 15, 0.7, 3.6, 0.1),
 "corn": ("Corn, sweet, yellow, raw", 86, 3.3, 18.7, 1.4),
 "mixed_veg": ("Vegetables, mixed, frozen, unprepared", 72, 3.3, 13.5, 0.5),
 "lemon": ("Lemon juice, raw", 22, 0.4, 6.9, 0.2),
 "honey": ("Honey", 304, 0.3, 82.4, 0.0),
 "almonds": ("Nuts, almonds", 579, 21.2, 21.6, 49.9),
 "walnuts": ("Nuts, walnuts, english", 654, 15.2, 13.7, 65.2),
 "chia": ("Seeds, chia seeds, dried", 486, 16.5, 42.1, 30.7),
 "cocoa": ("Cocoa, dry powder, unsweetened", 228, 19.6, 57.9, 13.7),
 "quinoa": ("Quinoa, cooked", 120, 4.4, 21.3, 1.9),
 "soy_sauce": ("Soy sauce made from soy (tamari)", 60, 10.5, 5.6, 0.1),
}

def I(key, grams, label):
    return {"key": key, "grams": grams, "label": label}

R = []
def recipe(id, name, meal, minutes, servings, tags, blurb, ingredients, steps):
    R.append(dict(id=id, name=name, meal=meal, minutes=minutes, servings=servings, tags=tags,
                  blurb=blurb, ingredients=ingredients, steps=steps))

# ---------------- Breakfast ----------------
recipe("protein-oats", "Protein overnight oats", "breakfast", 5, 1, ["no-cook", "meal-prep", "budget"],
  "Make it the night before and grab it on the way to the gym.",
  [I("oats",60,"60 g rolled oats"), I("milk",200,"200 ml low-fat milk"), I("greek_yogurt",100,"100 g Greek yogurt"),
   I("chia",10,"1 tbsp chia seeds"), I("blueberries",80,"½ cup blueberries"), I("honey",10,"2 tsp honey")],
  ["Mix oats, milk, yogurt and chia in a jar.", "Cover and refrigerate overnight (at least 4 hours).",
   "Top with blueberries and honey in the morning."])
recipe("veggie-egg-scramble", "Spinach & pepper egg scramble", "breakfast", 10, 1, ["vegetarian", "cut"],
  "High protein, very few calories: ideal when cutting.",
  [I("egg",100,"2 eggs"), I("egg_white",132,"4 egg whites"), I("spinach",60,"2 handfuls spinach"),
   I("red_pepper",75,"½ red pepper, diced"), I("olive_oil",5,"1 tsp olive oil"), I("bread_ww",40,"1 slice whole-wheat bread")],
  ["Heat the oil in a non-stick pan and soften the pepper for 3 minutes.", "Add the spinach until it wilts.",
   "Pour in the whisked eggs and whites, stir gently until just set.", "Serve with toasted bread."])
recipe("pb-banana-toast", "Peanut butter banana toast", "breakfast", 5, 1, ["vegetarian", "budget", "bulk"],
  "Quick, cheap calories for bulking or long training days.",
  [I("bread_ww",80,"2 slices whole-wheat bread"), I("peanut_butter",32,"2 tbsp peanut butter"),
   I("banana",118,"1 medium banana"), I("milk",250,"1 glass low-fat milk")],
  ["Toast the bread.", "Spread peanut butter and top with sliced banana.", "Serve with a glass of milk."])
recipe("cottage-pancakes", "Cottage cheese oat pancakes", "breakfast", 15, 1, ["vegetarian", "high-protein"],
  "Fluffy pancakes with 40 g+ protein and no protein powder.",
  [I("oats",50,"50 g oats"), I("cottage",150,"150 g cottage cheese"), I("egg",100,"2 eggs"),
   I("strawberries",100,"1 cup strawberries"), I("honey",10,"2 tsp honey")],
  ["Blend oats, cottage cheese and eggs until smooth.", "Cook small pancakes in a non-stick pan, 2 minutes per side.",
   "Serve with strawberries and a drizzle of honey."])
recipe("mandioca-egg-breakfast", "Mandioca with eggs & tomato", "breakfast", 25, 1, ["budget", "gluten-free"],
  "A filling Paraguayan-style breakfast: boiled mandioca, eggs and fresh tomato.",
  [I("cassava",150,"150 g mandioca (cassava)"), I("egg",150,"3 eggs"), I("tomato",100,"1 tomato"),
   I("olive_oil",5,"1 tsp oil")],
  ["Peel the mandioca, cut into chunks and boil in salted water 20 minutes until tender.",
   "Fry or scramble the eggs in the oil.", "Serve with the mandioca and sliced tomato."])

# ---------------- Pre-workout ----------------
recipe("banana-rice-cakes", "Banana & honey rice cakes", "pre-workout", 3, 1, ["vegetarian", "no-cook", "gluten-free"],
  "Fast carbs that are easy on the stomach 30–60 minutes before training.",
  [I("rice_cake",27,"3 rice cakes"), I("banana",118,"1 banana, sliced"), I("honey",10,"2 tsp honey"),
   I("peanut_butter",8,"½ tbsp peanut butter")],
  ["Spread a thin layer of peanut butter on the rice cakes.", "Top with banana slices and honey."])
recipe("yogurt-berry-bowl", "Greek yogurt berry bowl", "pre-workout", 3, 1, ["vegetarian", "no-cook", "cut"],
  "Light protein and carbs that won't sit heavy in the gym.",
  [I("greek_yogurt",200,"200 g Greek yogurt"), I("blueberries",75,"½ cup blueberries"),
   I("strawberries",75,"½ cup strawberries"), I("oats",20,"2 tbsp oats"), I("honey",7,"1 tsp honey")],
  ["Spoon yogurt into a bowl.", "Top with berries, oats and honey."])
recipe("sweet-potato-toast", "Sweet potato & egg toast", "pre-workout", 15, 1, ["gluten-free", "vegetarian"],
  "Slow carbs plus a little protein, about 90 minutes before a session.",
  [I("sweet_potato",150,"1 small sweet potato"), I("egg",100,"2 eggs"), I("spinach",30,"1 handful spinach")],
  ["Slice the sweet potato lengthways about 1 cm thick and toast or bake at 200 °C for 12 minutes.",
   "Fry or poach the eggs.", "Top the sweet potato slices with spinach and eggs."])

# ---------------- Post-workout ----------------
recipe("recovery-shake", "Chocolate recovery shake", "post-workout", 3, 1, ["no-cook", "vegetarian", "high-protein"],
  "Protein plus carbs right after training.",
  [I("whey",30,"1 scoop whey protein"), I("milk",300,"300 ml low-fat milk"), I("banana",118,"1 banana"),
   I("oats",30,"30 g oats"), I("cocoa",5,"1 tsp cocoa powder")],
  ["Blend everything with a few ice cubes until smooth."])
recipe("no-powder-shake", "No-powder protein shake", "post-workout", 3, 1, ["no-cook", "vegetarian", "budget"],
  "Same recovery idea without protein powder.",
  [I("greek_yogurt",200,"200 g Greek yogurt"), I("milk",200,"200 ml low-fat milk"), I("banana",118,"1 banana"),
   I("peanut_butter",16,"1 tbsp peanut butter"), I("honey",7,"1 tsp honey")],
  ["Blend everything until smooth. Add ice if you like it cold."])
recipe("chicken-rice-bowl", "Classic chicken, rice & broccoli", "post-workout", 25, 1, ["meal-prep", "gluten-free", "high-protein"],
  "The gym classic for a reason: lean protein, carbs and greens.",
  [I("chicken",170,"170 g chicken breast"), I("rice_white",200,"1 cup cooked white rice"), I("broccoli",150,"1½ cups broccoli"),
   I("olive_oil",7,"½ tbsp olive oil"), I("soy_sauce",15,"1 tbsp soy sauce"), I("garlic",6,"2 garlic cloves")],
  ["Season the chicken and cook in the oil with garlic, 6–7 minutes per side.",
   "Steam the broccoli for 5 minutes.", "Slice the chicken and serve over rice with broccoli and soy sauce."])
recipe("tuna-pasta", "Tuna tomato pasta", "post-workout", 15, 1, ["budget", "high-protein"],
  "Pantry ingredients, ready in 15 minutes.",
  [I("pasta",220,"220 g cooked pasta (about 80 g dry)"), I("tuna",120,"1 can tuna in water, drained"),
   I("tomato_canned",200,"200 g canned tomatoes"), I("onion",50,"½ onion"), I("garlic",6,"2 garlic cloves"),
   I("olive_oil",7,"½ tbsp olive oil"), I("parmesan",10,"1 tbsp grated parmesan")],
  ["Cook the pasta.", "Soften onion and garlic in the oil, add tomatoes and simmer 8 minutes.",
   "Stir in tuna and pasta, top with parmesan."])

# ---------------- Lunch ----------------
recipe("beef-burrito-bowl", "Lean beef burrito bowl", "lunch", 20, 1, ["gluten-free", "bulk"],
  "Big, satisfying bowl for building muscle.",
  [I("beef90",150,"150 g lean ground beef"), I("rice_brown",180,"¾ cup cooked brown rice"), I("black_beans",100,"½ cup black beans"),
   I("corn",50,"¼ cup corn"), I("tomato",80,"1 small tomato, diced"), I("avocado",50,"⅓ avocado"), I("lemon",10,"squeeze of lime/lemon")],
  ["Brown the beef with salt, cumin and paprika.", "Warm the beans and corn.",
   "Build the bowl: rice, beans, corn, beef, tomato and avocado. Finish with lemon juice."])
recipe("chickpea-feta-salad", "Chickpea, cucumber & feta salad", "lunch", 10, 1, ["vegetarian", "no-cook", "mediterranean"],
  "Mediterranean salad with plant protein and good fats.",
  [I("chickpeas",160,"1 cup cooked chickpeas"), I("cucumber",100,"½ cucumber"), I("tomato",100,"1 tomato"),
   I("onion",30,"¼ red onion"), I("feta",40,"40 g feta"), I("olive_oil",10,"2 tsp olive oil"), I("lemon",15,"1 tbsp lemon juice"),
   I("greek_yogurt",100,"100 g Greek yogurt (on the side)")],
  ["Chop the vegetables.", "Toss with chickpeas, crumbled feta, olive oil and lemon.",
   "Serve with Greek yogurt on the side for extra protein."])
recipe("chicken-wrap", "Chicken avocado wrap", "lunch", 15, 1, ["high-protein"],
  "Portable lunch for busy days.",
  [I("tortilla",70,"1 large flour tortilla"), I("chicken",130,"130 g chicken breast"), I("avocado",50,"⅓ avocado"),
   I("spinach",30,"1 handful spinach"), I("tomato",60,"½ tomato"), I("greek_yogurt",30,"2 tbsp Greek yogurt")],
  ["Cook and slice the chicken.", "Spread yogurt and mashed avocado on the tortilla.",
   "Add spinach, tomato and chicken, then roll up tightly."])
recipe("lentil-stew", "Hearty lentil stew (4 portions)", "lunch", 40, 4, ["vegetarian", "budget", "meal-prep", "gluten-free"],
  "Cook once, eat four times. Cheap, high in fibre and plant protein.",
  [I("lentils",800,"4 cups cooked lentils (about 300 g dry)"), I("carrot",150,"2 carrots"), I("onion",150,"1 large onion"),
   I("tomato_canned",400,"1 can chopped tomatoes"), I("potato",300,"2 potatoes"), I("olive_oil",20,"1½ tbsp olive oil"),
   I("garlic",12,"4 garlic cloves"), I("egg",200,"4 boiled eggs (1 per portion)")],
  ["Soften onion, carrot and garlic in the oil for 5 minutes.",
   "Add diced potato, tomatoes, lentils and water to cover; simmer 25 minutes.",
   "Season with salt, cumin and paprika. Serve each portion with a boiled egg."])

# ---------------- Dinner ----------------
recipe("salmon-potatoes", "Lemon salmon with potatoes & greens", "dinner", 30, 1, ["gluten-free", "mediterranean"],
  "Omega-3s and quality protein for recovery.",
  [I("salmon",150,"150 g salmon fillet"), I("potato",200,"2 small potatoes"), I("broccoli",120,"1 cup broccoli"),
   I("olive_oil",5,"1 tsp olive oil"), I("lemon",15,"1 tbsp lemon juice")],
  ["Boil the potatoes for 15 minutes.", "Bake the salmon at 200 °C for 12–15 minutes with lemon and salt.",
   "Steam the broccoli and toss the potatoes with olive oil."])
recipe("turkey-stir-fry", "Turkey veggie stir-fry", "dinner", 20, 1, ["high-protein"],
  "Lean protein and a big plate of vegetables.",
  [I("turkey93",150,"150 g lean ground turkey"), I("mixed_veg",200,"2 cups frozen mixed vegetables"),
   I("rice_white",130,"⅔ cup cooked rice"), I("soy_sauce",15,"1 tbsp soy sauce"), I("garlic",6,"2 garlic cloves"),
   I("olive_oil",5,"1 tsp oil")],
  ["Brown the turkey with garlic in the oil.", "Add the vegetables and stir-fry 6–8 minutes.",
   "Season with soy sauce and serve over rice."])
recipe("tofu-quinoa-bowl", "Crispy tofu quinoa bowl", "dinner", 25, 1, ["vegetarian", "gluten-free"],
  "Complete plant protein from tofu and quinoa.",
  [I("tofu",200,"200 g firm tofu"), I("quinoa",185,"1 cup cooked quinoa"), I("broccoli",100,"1 cup broccoli"),
   I("red_pepper",75,"½ red pepper"), I("soy_sauce",15,"1 tbsp soy sauce"), I("olive_oil",7,"½ tbsp oil")],
  ["Press the tofu, cube it and pan-fry in the oil until golden, about 10 minutes.",
   "Stir-fry the broccoli and pepper.", "Serve over quinoa with soy sauce."])
recipe("beef-pasta-bolognese", "Lean beef bolognese (4 portions)", "dinner", 35, 4, ["meal-prep", "bulk"],
  "Family-size batch that freezes well.",
  [I("beef90",500,"500 g lean ground beef"), I("pasta",900,"900 g cooked pasta (about 320 g dry)"),
   I("tomato_canned",800,"2 cans chopped tomatoes"), I("onion",150,"1 large onion"), I("carrot",100,"1 carrot"),
   I("garlic",12,"4 garlic cloves"), I("olive_oil",15,"1 tbsp olive oil"), I("parmesan",40,"40 g parmesan")],
  ["Soften onion, carrot and garlic in the oil.", "Add the beef and brown it.",
   "Add tomatoes and simmer 20 minutes.", "Serve over pasta with parmesan."])
recipe("chicken-mandioca", "Chicken with mandioca & salad", "dinner", 35, 1, ["gluten-free", "budget"],
  "Local-style plate: grilled chicken, boiled mandioca and a fresh salad.",
  [I("chicken",170,"170 g chicken breast"), I("cassava",150,"150 g mandioca"), I("tomato",100,"1 tomato"),
   I("onion",30,"¼ onion"), I("lemon",10,"lemon juice"), I("olive_oil",7,"½ tbsp olive oil")],
  ["Boil the peeled mandioca in salted water for 20 minutes.",
   "Grill the seasoned chicken 6–7 minutes per side.", "Toss tomato and onion with lemon and oil."])

# ---------------- Snacks ----------------
recipe("cottage-fruit", "Cottage cheese & fruit", "snack", 2, 1, ["vegetarian", "no-cook", "cut"],
  "Slow-digesting protein. Good before bed.",
  [I("cottage",200,"200 g cottage cheese"), I("strawberries",100,"1 cup strawberries"), I("walnuts",10,"a few walnuts")],
  ["Top cottage cheese with fruit and walnuts."])
recipe("boiled-eggs-apple", "Boiled eggs & apple", "snack", 12, 1, ["vegetarian", "budget", "gluten-free"],
  "Cheap, portable protein snack.",
  [I("egg",100,"2 boiled eggs"), I("apple",180,"1 apple")],
  ["Boil eggs for 9 minutes, cool in cold water, peel.", "Eat with an apple."])
recipe("trail-mix-yogurt", "Yogurt with nuts & honey", "snack", 2, 1, ["vegetarian", "no-cook"],
  "Easy extra calories and protein between meals.",
  [I("greek_yogurt",170,"170 g Greek yogurt"), I("almonds",25,"a small handful of almonds"), I("honey",10,"2 tsp honey"),
   I("chia",5,"1 tsp chia")],
  ["Stir honey into the yogurt and top with almonds and chia."])
recipe("sardine-toast", "Sardines on toast", "snack", 5, 1, ["budget", "high-protein"],
  "Protein, omega-3 and calcium from the bones, for very little money.",
  [I("bread_ww",80,"2 slices whole-wheat bread"), I("sardines",90,"1 can sardines, drained"), I("tomato",60,"½ tomato"),
   I("lemon",5,"squeeze of lemon")],
  ["Toast the bread.", "Mash sardines with lemon and pile onto the toast with sliced tomato."])
recipe("ricotta-protein-pudding", "Chocolate ricotta protein pudding", "snack", 5, 1, ["vegetarian", "no-cook", "high-protein"],
  "Dessert that helps you hit your protein target.",
  [I("ricotta",150,"150 g part-skim ricotta"), I("whey",15,"½ scoop whey protein"), I("cocoa",8,"1 tbsp cocoa"),
   I("honey",10,"2 tsp honey"), I("strawberries",60,"a few strawberries")],
  ["Whisk ricotta, whey, cocoa and honey until smooth.", "Chill 10 minutes and top with strawberries."])

out = []
for r in R:
    tot = [0.0, 0.0, 0.0, 0.0]
    for ing in r["ingredients"]:
        _, kcal, p, c, f = USDA[ing["key"]]
        g = ing["grams"] / 100
        for i, v in enumerate((kcal, p, c, f)): tot[i] += v * g
    s = r["servings"]
    kcal, p, c, f = (round(x / s) for x in tot)
    tags = set(r["tags"])
    if p * 4 / max(kcal, 1) >= 0.30: tags.add("high-protein")
    if kcal <= 450 and p >= 25: tags.add("cut")
    if kcal >= 600: tags.add("bulk")
    r2 = dict(r)
    r2["tags"] = sorted(tags)
    r2["kcal"], r2["protein"], r2["carbs"], r2["fat"] = kcal, p, c, f
    r2["ingredients"] = [dict(label=i["label"], grams=i["grams"], usda=USDA[i["key"]][0]) for i in r["ingredients"]]
    out.append(r2)

json.dump(out, open(sys.argv[1], "w"), ensure_ascii=False, indent=1)
for r in out:
    print(f'{r["meal"]:13} {r["name"][:40]:40} {r["kcal"]:4} kcal P{r["protein"]:3} C{r["carbs"]:3} F{r["fat"]:3}  {",".join(r["tags"])}')
