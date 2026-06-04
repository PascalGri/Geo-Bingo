// App Store 6.9" preview frames (iPhone 1320×2868).
// Each entry drives one marketing screenshot rendered by frame.html.
//   t1  = white headline line
//   t2  = fuchsia-gradient headline line (optional)
//   sub = lavender subtitle
//   shot = filename in screenshots/ (a clean 1320×2868 app capture)
//
// IMPORTANT (Apple Guideline 2.3.7): no price words in any subtitle/headline —
// no "kostenlos", "gratis", "free", "%", "Rabatt", "sparen", "Angebot".
window.FRAMES = [
  { t1: "Gewinne echte", t2: "Prämien",     sub: "Sammle Sterne, sichere dir Belohnungen", shot: "praemien.png" },
  { t1: "Dein Alltag",   t2: "wird zum Spiel", sub: "Foto-Bingo mit Freunden, weltweit",   shot: "home.png" },
  { t1: "5×5 Aufgaben.", t2: "1 Gewinner.",  sub: "Erfülle Challenges in Echtzeit",        shot: "game.png" },
  { t1: "Solo gegen",    t2: "die KI",       sub: "Lass deine Fotos in Echtzeit bewerten",  shot: "solo.png" },
  { t1: "Style deinen",  t2: "Spieler",      sub: "Banner, Rahmen & Effekte im Shop",       shot: "shop.png" }
];
