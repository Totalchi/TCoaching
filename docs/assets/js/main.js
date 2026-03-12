const runtimeConfig = window.TCOACHING_CONFIG || {};

const normalizeBaseUrl = (value) => {
  if (!value) {
    return '';
  }
  return String(value).trim().replace(/\/+$/, '');
};

const buildApiUrl = (path) => {
  const trimmedPath = path.startsWith('/') ? path : `/${path}`;
  const baseUrl = normalizeBaseUrl(runtimeConfig.apiBaseUrl);
  return baseUrl ? `${baseUrl}${trimmedPath}` : trimmedPath;
};

const apiConfig = {
  enabled: runtimeConfig.apiEnabled !== false,
  contactEndpoint: runtimeConfig.contactEndpoint || buildApiUrl('/api/contact'),
  trackEndpoint: runtimeConfig.trackEndpoint || buildApiUrl('/api/track'),
  publicConfigEndpoint: runtimeConfig.publicConfigEndpoint || buildApiUrl('/api/public-config'),
  contactEmail: runtimeConfig.contactEmail || 'hello@tcoaching.be',
  bookingUrl: runtimeConfig.bookingUrl || '',
  mode: runtimeConfig.mode || 'app'
};
const trackingWindowMs = 15 * 60 * 1000;

const translations = {
  nl: {
    'nav.home': 'Home overzicht',
    'nav.life': 'Life Coaching trajecten',
    'nav.pt': 'Personal Training sessies',
    'nav.stress': 'Stress & Burnout hulp',
    'nav.assertive': 'Assertiviteit training',
    'nav.pricing': 'Prijzen en pakketten',
    'nav.contact': 'Contact & afspraken',
    'toggle.theme.light': 'Ochtendgloed',
    'toggle.theme.dark': 'Maanlicht',
    'lang.nl': 'Nederlands',
    'lang.en': 'Engels',
    'cta.intake': 'Plan gratis intakegesprek',
    'cta.packages': 'Bekijk alle pakketten',
    'cta.learn': 'Ontdek meer details',
    'cta.book': 'Boek een sessie op maat',
    'cta.contact': 'Neem direct contact op',
    'cta.requestIntake': 'Vraag je intake aan',
    'home.hero.title': 'Leven met richting, rust en krachtige actie',
    'home.hero.subtitle': 'Life coaching en personal training voor beginners en groei. We bouwen aan gezonde routines, sterker zelfvertrouwen en keuzes die echt bij jou passen, zodat je elke week merkt dat je vooruitgaat.',
    'home.hero.note': 'Focus op balans, energie en duurzame verandering die je ook volhoudt.',
    'home.hero.card.title': 'Jouw volgende stap in groei',
    'home.hero.card.line1': '1-op-1 begeleiding op maat, zonder ruis',
    'home.hero.card.line2': 'Heldere doelen met een haalbaar plan per week',
    'home.hero.card.line3': 'Warm, professioneel en resultaatgericht, met echte opvolging',
    'pill.life': 'Life Coaching focus',
    'pill.pt': 'Personal Training focus',
    'pill.stress': 'Stress & Burnout steun',
    'pill.assertive': 'Assertiviteit skills',
    'pill.pricing': 'Prijzen overzicht',
    'pill.contact': 'Contact & intake',
    'photo.placeholder': 'Coachfoto komt hier',
    'home.focus.title': 'Kernfocus van jouw traject',
    'home.focus.life.title': 'Life Coaching met richting',
    'home.focus.life.desc': 'Richting vinden, keuzes maken, mindset versterken en grenzen bewaken, stap voor stap.',
    'home.focus.pt.title': 'Personal Training met vertrouwen',
    'home.focus.pt.desc': 'Voor beginners en medium niveau met veilige, motiverende opbouw die je energie geeft.',
    'home.focus.stress.title': 'Stress & Burnout herstel',
    'home.focus.stress.desc': 'Rust, herstel en terug naar energie met heldere, haalbare stappen.',
    'home.method.title': 'Werkwijze in drie fases',
    'home.method.step1.title': 'Gratis intakegesprek',
    'home.method.step1.desc': 'We luisteren, verkennen en bepalen samen jouw focus en behoeften.',
    'home.method.step2.title': 'Persoonlijk plan op maat',
    'home.method.step2.desc': 'Heldere doelen, acties en routines die werken voor jouw leven.',
    'home.method.step3.title': 'Coaching met ritme en support',
    'home.method.step3.desc': 'Consistentie, support en bijsturing wanneer nodig, met duidelijke check-ins.',
    'home.results.title': 'Wat je mag verwachten in de praktijk',
    'home.results.item1': 'Meer energie en mentale helderheid in je dagelijkse keuzes',
    'home.results.item2': 'Gezonde routines die je volhoudt, ook in drukke weken',
    'home.results.item3': 'Sterke grenzen en assertieve communicatie zonder schuldgevoel',
    'home.results.item4': 'Zelfvertrouwen in lichaam, mindset en keuzes',
    'home.banner.title': 'Klaar voor een gratis intakegesprek dat echt helderheid geeft?',
    'home.banner.subtitle': 'Ontdek welke aanpak het beste bij jou past en wat nu het meest helpt.',
    'home.banner.button': 'Plan je intake',
    'home.form.title': 'Vraag je intake aan en start rustig',
    'home.form.subtitle': 'Laat je gegevens achter en ik neem snel persoonlijk contact op.',
    'home.hero.stat1.value': '1-op-1',
    'home.hero.stat1.label': 'Begeleiding op maat',
    'home.hero.stat2.value': '30 min',
    'home.hero.stat2.label': 'Gratis intake',
    'home.hero.stat3.value': 'Rust + actie',
    'home.hero.stat3.label': 'Mentale en fysieke groei',
    'home.services.title': 'Vier trajecten, elk met een eigen ritme',
    'home.services.subtitle': 'Geen generieke sessies, maar begeleiding die past bij je vraag, energie en tempo.',
    'home.services.life.point1': 'Richting en keuzes zonder te blijven twijfelen',
    'home.services.life.point2': 'Grenzen, zelfbeeld en dagelijkse rust versterken',
    'home.services.pt.point1': 'Veilige opbouw voor beginners en herstarters',
    'home.services.pt.point2': 'Kracht, conditie en routines die je volhoudt',
    'home.services.stress.point1': 'Herstel met ruimte, structuur en duidelijke grenzen',
    'home.services.stress.point2': 'Terug naar helderheid zonder jezelf te forceren',
    'home.services.assert.title': 'Assertiviteit in echte situaties',
    'home.services.assert.desc': 'Leren spreken, kiezen en begrenzen zonder hard te worden.',
    'home.services.assert.point1': 'Moeilijke gesprekken rustig en duidelijk voeren',
    'home.services.assert.point2': 'Respectvol nee zeggen zonder schuldgevoel',
    'home.proof.title': 'Wat professioneel samenwerken hier concreet betekent',
    'home.proof.card1.title': 'Heldere intake en volgende stap',
    'home.proof.card1.desc': 'Je krijgt snel zicht op wat nu prioriteit heeft, zonder vaag traject of verkooppraat.',
    'home.proof.card2.title': 'Sessies die ook buiten de afspraak werken',
    'home.proof.card2.desc': 'We vertalen inzichten naar routines, afspraken en gedrag dat in je week past.',
    'home.proof.card3.title': 'Warmte zonder vrijblijvendheid',
    'home.proof.card3.desc': 'Menselijk contact, duidelijke feedback en opvolging wanneer het moet schuiven.',
    'home.testimonials.title': 'Waar mensen meestal het verschil voelen',
    'home.testimonial1.quote': 'Ik voelde voor het eerst dat er rust én richting tegelijk mogelijk waren.',
    'home.testimonial1.name': 'Cliënt life coaching',
    'home.testimonial1.meta': 'Meer focus, minder overthinking',
    'home.testimonial2.quote': 'De training was haalbaar, duidelijk en gaf me opnieuw vertrouwen in mijn lichaam.',
    'home.testimonial2.name': 'Cliënt personal training',
    'home.testimonial2.meta': 'Veilige herstart met structuur',
    'home.testimonial3.quote': 'Ik heb geleerd grenzen uit te spreken zonder conflict te zoeken of mezelf kwijt te raken.',
    'home.testimonial3.name': 'Cliënt assertiviteit',
    'home.testimonial3.meta': 'Sterker communiceren op werk en thuis',
    'home.faq.title': 'Praktische vragen die vaak terugkomen',
    'home.faq1.q': 'Is een intake echt vrijblijvend?',
    'home.faq1.a': 'Ja. De intake dient om je vraag helder te krijgen en te bekijken of er een goede match is. Je hangt nergens aan vast.',
    'home.faq2.q': 'Kan ik coaching en training combineren?',
    'home.faq2.a': 'Ja. Voor veel trajecten werkt de combinatie van mindset, beweging en structuur net het sterkst, zolang het haalbaar blijft.',
    'home.faq3.q': 'Voor welk niveau is personal training bedoeld?',
    'home.faq3.a': 'Vooral voor beginners en mensen die opnieuw willen starten. Techniek, veiligheid en vertrouwen komen altijd eerst.',
    'home.faq4.q': 'Hoe snel krijg ik antwoord na een aanvraag?',
    'home.faq4.a': 'Op werkdagen volgt normaal binnen 24 uur een persoonlijk antwoord om een intake of vervolgstap af te stemmen.',
    'home.cta.title': 'Klaar om je eerste stap professioneel en rustig te zetten?',
    'home.cta.subtitle': 'Plan een gratis intake, vertel waar je vastloopt en we bekijken samen wat op dit moment het meeste oplevert.',
    'home.cta.button': 'Plan je gratis intake',
    'home.form.note.title': 'Wat je na je aanvraag mag verwachten',
    'home.form.note.subtitle': 'Je krijgt een persoonlijk antwoord, geen automatische funnel. Eerst helderheid, dan pas een trajectvoorstel.',
    'life.hero.title': 'Life Coaching met richting',
    'life.hero.subtitle': 'Voor mensen die richting zoeken, patronen willen doorbreken en hun leven bewust willen vormgeven, met realistische stappen en ondersteuning.',
    'life.who.title': 'Voor wie is dit geschikt?',
    'life.who.item1': 'Je wil keuzes maken die bij je waarden passen en dat ook durven uitspreken.',
    'life.who.item2': 'Je zoekt meer rust en focus in je leven, zonder jezelf te verliezen.',
    'life.who.item3': 'Je wil sterker worden in grenzen, zelfbeeld en dagelijkse zelfzorg.',
    'life.offer.title': 'Wat je krijgt in het traject',
    'life.offer.card1.title': 'Diepgaande gesprekken met richting',
    'life.offer.card1.desc': 'Samen brengen we helderheid in je doelen, blokkades en keuzes die je tegenhouden.',
    'life.offer.card2.title': 'Actiegerichte stappen en ritme',
    'life.offer.card2.desc': 'Concreet plan met routines, accountability en haalbare weekdoelen.',
    'life.offer.card3.title': 'Zachte kracht en vertrouwen',
    'life.offer.card3.desc': 'Warmte en professionaliteit die je vooruit helpen, ook als het even schuurt.',
    'life.cta.title': 'Start met een intakegesprek op jouw tempo',
    'life.cta.subtitle': 'Je zit nergens aan vast, we ontdekken samen wat past en wat je nu nodig hebt.',
    'life.cta.button': 'Plan je intake',
    'pt.hero.title': 'Personal Training met begeleiding',
    'pt.hero.subtitle': 'Voor beginners en medium niveau die gezonder willen leven met veilige, motiverende training, begeleiding en plezier.',
    'pt.focus.title': 'Focus van de trainingen en coaching',
    'pt.focus.item1': 'Techniek en blessurepreventie zodat je veilig groeit',
    'pt.focus.item2': 'Opbouw van kracht en conditie met realistische progressie',
    'pt.focus.item3': 'Voeding en lifestyle routines die je echt volhoudt',
    'pt.program.title': 'Zo pakken we het aan, stap voor stap',
    'pt.program.desc': 'Persoonlijke sessies met duidelijke progressie, afgestemd op jouw tempo, doelen en agenda.',
    'pt.cta.title': 'Klaar voor de eerste stap met begeleiding?',
    'pt.cta.subtitle': 'Plan een gratis intake en ontdek jouw startpunt plus een helder plan.',
    'stress.hero.title': 'Stress & Burnout begeleiding met herstel',
    'stress.hero.subtitle': 'Rust in je hoofd, energie in je dag en weer grip op je leven met duidelijke grenzen.',
    'stress.signals.title': 'Herkenbare signalen en alarmbellen',
    'stress.signals.item1': 'Altijd moe of een opgejaagd gevoel, zelfs na rust',
    'stress.signals.item2': 'Slecht slapen, piekeren en wakker worden zonder herstel',
    'stress.signals.item3': 'Geen ruimte voor jezelf, herstel en echte pauzes',
    'stress.support.title': 'Begeleiding met structuur en zachte opbouw',
    'stress.support.desc': 'We werken aan herstel, grenzen, rustmomenten en een realistisch plan dat je kan volhouden.',
    'stress.cta.title': 'Pak stress aan met een helder plan dat rust brengt',
    'assert.hero.title': 'Assertiviteit met zelfvertrouwen',
    'assert.hero.subtitle': 'Leren nee zeggen, helder communiceren en jezelf serieus nemen in elke situatie.',
    'assert.learn.title': 'Wat je leert en oefent',
    'assert.learn.item1': 'Grenzen aangeven zonder schuldgevoel en met respect',
    'assert.learn.item2': 'Zichtbaar en duidelijk communiceren, ook onder druk',
    'assert.learn.item3': 'Zelfvertrouwen in moeilijke gesprekken en lastige keuzes',
    'assert.practice.title': 'Praktisch, toepasbaar en nuchter',
    'assert.practice.desc': 'Oefeningen en reflectie die je direct kan inzetten in werk, relaties en dagelijks leven.',
    'pricing.hero.title': 'Prijspakketten met begeleiding',
    'pricing.hero.subtitle': 'Transparante pakketten met focus op resultaat, begeleiding en haalbare stappen.',
    'pricing.card1.title': 'Start traject',
    'pricing.card1.price': 'EUR 249',
    'pricing.card1.desc': '2 sessies per maand + kort opvolgmoment om bij te sturen.',
    'pricing.card1.item1': 'Persoonlijk plan met weekfocus',
    'pricing.card1.item2': 'WhatsApp support voor vragen onderweg',
    'pricing.card2.title': 'Focus traject',
    'pricing.card2.price': 'EUR 389',
    'pricing.card2.desc': '4 sessies per maand + tussentijdse coaching en feedback.',
    'pricing.card2.item1': 'Doelen per week met check-ins',
    'pricing.card2.item2': 'Voeding, mindset en herstelroutines',
    'pricing.card3.title': 'Momentum traject',
    'pricing.card3.price': 'EUR 529',
    'pricing.card3.desc': '6 sessies per maand + intensieve opvolging en planning.',
    'pricing.card3.item1': 'Diepgaande trajecten met verdieping',
    'pricing.card3.item2': 'Prioriteit in planning en contact',
    'pricing.intake.title': 'Gratis intakegesprek van 30 min',
    'pricing.intake.price': 'Gratis kennismaking',
    'pricing.intake.desc': '30 minuten kennismaken en bepalen wat bij je past en wat nu helpt.',
    'pricing.note': 'Pakketten zijn combineerbaar met personal training of life coaching, afgestemd op je doel.',
    'pricing.cta': 'Plan je intake',
    'contact.hero.title': 'Contact & afspraken met persoonlijke opvolging',
    'contact.hero.subtitle': 'Vraag je gesprek aan en zet de eerste stap naar verandering met duidelijke afspraken.',
    'contact.info.title': 'Contactgegevens en bereikbaarheid',
    'contact.info.desc': 'Ik reageer binnen 24 uur op werkdagen en denk graag met je mee.',
    'contact.info.emailLabel': 'Emailadres',
    'contact.info.phoneLabel': 'Telefoonnummer',
    'contact.info.locationLabel': 'Locatie en regio',
    'contact.form.title': 'Boek je intake en plan een moment',
    'contact.form.subtitle': 'Vul het formulier in, dan plannen we samen een moment dat past bij je agenda.',
    'form.name': 'Volledige naam',
    'form.email': 'Emailadres',
    'form.phone': 'Telefoonnummer',
    'form.topic': 'Focusgebied',
    'form.goal': 'Jouw doel of uitdaging',
    'form.time': 'Gewenste tijdstippen en dagen',
    'form.message': 'Extra info of context',
    'form.submit': 'Verstuur je aanvraag',
    'form.statusIdle': 'We behandelen je aanvraag persoonlijk en met zorg.',
    'form.statusSending': 'Even wachten, je aanvraag wordt veilig verstuurd.',
    'form.statusSuccess': 'Dank je, je aanvraag is verstuurd en je hoort snel van mij.',
    'form.statusError': 'Er ging iets mis. Probeer opnieuw of mail direct naar ons.',
    'form.statusCaptcha': 'Bevestig de captcha om je aanvraag te versturen.',
    'form.statusConfigMissing': 'Formulier is tijdelijk niet beschikbaar. Probeer later opnieuw of stuur een mail.',
    'option.life': 'Life coaching traject',
    'option.pt': 'Personal training sessies',
    'option.stress': 'Stress & burnout herstel',
    'option.assertive': 'Assertiviteit training',
    'option.other': 'Andere vraag of onderwerp',
    'popup.title': 'Gratis intakegesprek plannen?',
    'popup.subtitle': 'Vraag een kennismaking aan en ontdek jouw volgende stap met duidelijkheid.',
    'popup.button': 'Ja, graag plannen',
    'footer.tagline': 'Warm, professioneel en doelgericht coachen met aandacht voor jouw tempo.',
    'footer.servicesTitle': 'Diensten en trajecten',
    'footer.linksTitle': 'Snelle links',
    'footer.contactTitle': 'Contact en info',
    'footer.locationTitle': 'Locatie en regio',
    'footer.rights': 'Alle rechten voorbehouden. TCoaching.'
  },
  en: {
    'nav.home': 'Home overview',
    'nav.life': 'Life Coaching paths',
    'nav.pt': 'Personal Training sessions',
    'nav.stress': 'Stress & Burnout support',
    'nav.assertive': 'Assertiveness training',
    'nav.pricing': 'Pricing and packages',
    'nav.contact': 'Contact & booking',
    'toggle.theme.light': 'Sunrise glow',
    'toggle.theme.dark': 'Moonlight',
    'lang.nl': 'Dutch',
    'lang.en': 'English',
    'cta.intake': 'Book a free intake call',
    'cta.packages': 'View all packages',
    'cta.learn': 'Explore the details',
    'cta.book': 'Book a tailored session',
    'cta.contact': 'Get in touch now',
    'cta.requestIntake': 'Request your intake',
    'home.hero.title': 'Live with direction, calm, and purposeful action',
    'home.hero.subtitle': 'Life coaching and personal training for beginners and growth. We build healthy routines, stronger confidence and choices that feel right, so you feel progress week after week.',
    'home.hero.note': 'Focused on balance, energy, and sustainable change you can keep.',
    'home.hero.card.title': 'Your next step in growth',
    'home.hero.card.line1': '1-on-1 coaching tailored to you, without noise',
    'home.hero.card.line2': 'Clear goals with a realistic weekly plan',
    'home.hero.card.line3': 'Warm, professional, and results driven with true follow up',
    'pill.life': 'Life Coaching focus',
    'pill.pt': 'Personal Training focus',
    'pill.stress': 'Stress & Burnout support',
    'pill.assertive': 'Assertiveness skills',
    'pill.pricing': 'Pricing overview',
    'pill.contact': 'Contact & intake',
    'photo.placeholder': 'Coach photo goes here',
    'home.focus.title': 'Core focus of your journey',
    'home.focus.life.title': 'Life Coaching with direction',
    'home.focus.life.desc': 'Find direction, make choices, strengthen mindset and boundaries, step by step.',
    'home.focus.pt.title': 'Personal Training with confidence',
    'home.focus.pt.desc': 'For beginners and medium level with safe, motivating progress that builds energy.',
    'home.focus.stress.title': 'Stress & Burnout recovery',
    'home.focus.stress.desc': 'Recover, restore energy, and regain clarity with practical steps.',
    'home.method.title': 'How it works in three phases',
    'home.method.step1.title': 'Free intake call',
    'home.method.step1.desc': 'We listen, explore, and define your focus and needs together.',
    'home.method.step2.title': 'Personal plan made for you',
    'home.method.step2.desc': 'Clear goals, actions, and routines that fit your real life.',
    'home.method.step3.title': 'Coaching rhythm and support',
    'home.method.step3.desc': 'Consistency, support, and adjustments when needed, with clear check ins.',
    'home.results.title': 'What to expect in practice',
    'home.results.item1': 'More energy and mental clarity in daily choices',
    'home.results.item2': 'Healthy routines you can keep, even in busy weeks',
    'home.results.item3': 'Strong boundaries and assertive communication without guilt',
    'home.results.item4': 'Confidence in body, mindset, and decisions',
    'home.banner.title': 'Ready for a free intake call that brings clarity?',
    'home.banner.subtitle': 'Discover which approach fits you best and what helps right now.',
    'home.banner.button': 'Plan your intake',
    'home.form.title': 'Request your intake and start calmly',
    'home.form.subtitle': 'Leave your details and I will personally follow up soon.',
    'home.hero.stat1.value': '1-on-1',
    'home.hero.stat1.label': 'Tailored guidance',
    'home.hero.stat2.value': '30 min',
    'home.hero.stat2.label': 'Free intake',
    'home.hero.stat3.value': 'Calm + action',
    'home.hero.stat3.label': 'Mental and physical growth',
    'home.services.title': 'Four tracks, each with its own rhythm',
    'home.services.subtitle': 'No generic sessions, but guidance that fits your question, energy, and pace.',
    'home.services.life.point1': 'Find direction and make choices without endless doubt',
    'home.services.life.point2': 'Strengthen boundaries, self image, and daily calm',
    'home.services.pt.point1': 'Safe progression for beginners and people restarting',
    'home.services.pt.point2': 'Build strength, fitness, and routines you can sustain',
    'home.services.stress.point1': 'Recover with space, structure, and clear boundaries',
    'home.services.stress.point2': 'Return to clarity without forcing yourself',
    'home.services.assert.title': 'Assertiveness in real situations',
    'home.services.assert.desc': 'Learn to speak up, choose, and set limits without becoming hard.',
    'home.services.assert.point1': 'Handle difficult conversations calmly and clearly',
    'home.services.assert.point2': 'Say no respectfully without guilt',
    'home.proof.title': 'What working professionally together means here',
    'home.proof.card1.title': 'Clear intake and next step',
    'home.proof.card1.desc': 'You quickly see what matters most now, without vague pathways or sales talk.',
    'home.proof.card2.title': 'Sessions that work beyond the appointment',
    'home.proof.card2.desc': 'We turn insight into routines, agreements, and behaviour that fit your week.',
    'home.proof.card3.title': 'Warmth without vagueness',
    'home.proof.card3.desc': 'Human contact, clear feedback, and follow up when something needs to move.',
    'home.testimonials.title': 'Where people usually feel the difference',
    'home.testimonial1.quote': 'For the first time I felt that calm and direction could exist together.',
    'home.testimonial1.name': 'Life coaching client',
    'home.testimonial1.meta': 'More focus, less overthinking',
    'home.testimonial2.quote': 'The training felt doable, clear, and gave me confidence in my body again.',
    'home.testimonial2.name': 'Personal training client',
    'home.testimonial2.meta': 'Safe restart with structure',
    'home.testimonial3.quote': 'I learned to express boundaries without seeking conflict or losing myself.',
    'home.testimonial3.name': 'Assertiveness client',
    'home.testimonial3.meta': 'Stronger communication at work and at home',
    'home.faq.title': 'Practical questions that come up often',
    'home.faq1.q': 'Is the intake really without obligation?',
    'home.faq1.a': 'Yes. The intake is there to clarify your question and check whether the fit is right. You are not committed to anything.',
    'home.faq2.q': 'Can I combine coaching and training?',
    'home.faq2.a': 'Yes. For many people, mindset, movement, and structure work best together, as long as the pace stays realistic.',
    'home.faq3.q': 'What level is personal training for?',
    'home.faq3.a': 'Mostly for beginners and people starting again. Technique, safety, and confidence always come first.',
    'home.faq4.q': 'How quickly will I get a reply after applying?',
    'home.faq4.a': 'On working days you will normally receive a personal reply within 24 hours to plan the next step.',
    'home.cta.title': 'Ready to take your first step in a calm, professional way?',
    'home.cta.subtitle': 'Book a free intake, share where you are stuck, and we will look at what creates the most value right now.',
    'home.cta.button': 'Plan your free intake',
    'home.form.note.title': 'What to expect after your request',
    'home.form.note.subtitle': 'You receive a personal reply, not an automated funnel. First clarity, then a tailored proposal.',
    'life.hero.title': 'Life Coaching with direction',
    'life.hero.subtitle': 'For people who want direction, want to break patterns, and design life with intention, supported by realistic steps.',
    'life.who.title': 'Is this a good fit for you?',
    'life.who.item1': 'You want choices that match your values and the courage to voice them.',
    'life.who.item2': 'You want more calm and focus without losing yourself.',
    'life.who.item3': 'You want stronger boundaries, self image, and daily self care.',
    'life.offer.title': 'What you get in the journey',
    'life.offer.card1.title': 'Deep conversations with direction',
    'life.offer.card1.desc': 'We bring clarity to your goals, blockers, and the choices holding you back.',
    'life.offer.card2.title': 'Action steps and rhythm',
    'life.offer.card2.desc': 'Concrete plan with routines, accountability, and weekly goals.',
    'life.offer.card3.title': 'Gentle strength and trust',
    'life.offer.card3.desc': 'Warmth and professionalism that move you forward, even when it is hard.',
    'life.cta.title': 'Start with an intake call at your pace',
    'life.cta.subtitle': 'No commitment, just clarity about what fits and what you need now.',
    'life.cta.button': 'Plan your intake',
    'pt.hero.title': 'Personal Training with guidance',
    'pt.hero.subtitle': 'For beginners and medium level who want to get healthier with safe, motivating training, guidance, and fun.',
    'pt.focus.title': 'Training and coaching focus',
    'pt.focus.item1': 'Technique and injury prevention so you grow safely',
    'pt.focus.item2': 'Build strength and conditioning with realistic progress',
    'pt.focus.item3': 'Nutrition and lifestyle routines you can truly keep',
    'pt.program.title': 'How we work, step by step',
    'pt.program.desc': 'Personal sessions with clear progress, adapted to your pace, goals, and schedule.',
    'pt.cta.title': 'Ready for the first step with guidance?',
    'pt.cta.subtitle': 'Book a free intake and discover your starting point plus a clear plan.',
    'stress.hero.title': 'Stress & Burnout recovery coaching',
    'stress.hero.subtitle': 'Calm in your mind, energy in your day, and control in your life with clear boundaries.',
    'stress.signals.title': 'Recognizable signals and warning signs',
    'stress.signals.item1': 'Always tired or rushed, even after rest',
    'stress.signals.item2': 'Poor sleep, worrying, and waking up without recovery',
    'stress.signals.item3': 'No space for yourself, recovery, or real pauses',
    'stress.support.title': 'Structured guidance with gentle buildup',
    'stress.support.desc': 'We work on recovery, boundaries, rest moments, and a realistic plan you can keep.',
    'stress.cta.title': 'Tackle stress with a clear plan that brings calm',
    'assert.hero.title': 'Assertiveness with confidence',
    'assert.hero.subtitle': 'Learn to say no, communicate clearly, and take yourself seriously in every situation.',
    'assert.learn.title': 'What you learn and practice',
    'assert.learn.item1': 'Set boundaries without guilt and with respect',
    'assert.learn.item2': 'Communicate clearly and visibly, even under pressure',
    'assert.learn.item3': 'Confidence in difficult conversations and tough choices',
    'assert.practice.title': 'Practical, usable, and grounded',
    'assert.practice.desc': 'Exercises and reflection you can apply immediately at work, in relationships, and in daily life.',
    'pricing.hero.title': 'Pricing packages with guidance',
    'pricing.hero.subtitle': 'Transparent packages focused on results, guidance, and realistic steps.',
    'pricing.card1.title': 'Start track',
    'pricing.card1.price': 'EUR 249',
    'pricing.card1.desc': '2 sessions per month + a short check in to adjust.',
    'pricing.card1.item1': 'Personal plan with weekly focus',
    'pricing.card1.item2': 'WhatsApp support for questions on the go',
    'pricing.card2.title': 'Focus track',
    'pricing.card2.price': 'EUR 389',
    'pricing.card2.desc': '4 sessions per month + in between coaching and feedback.',
    'pricing.card2.item1': 'Weekly goals with check ins',
    'pricing.card2.item2': 'Nutrition, mindset, and recovery routines',
    'pricing.card3.title': 'Momentum track',
    'pricing.card3.price': 'EUR 529',
    'pricing.card3.desc': '6 sessions per month + intensive follow up and planning.',
    'pricing.card3.item1': 'Deep coaching tracks with extra depth',
    'pricing.card3.item2': 'Priority in planning and contact',
    'pricing.intake.title': 'Free 30 min intake call',
    'pricing.intake.price': 'Free intro',
    'pricing.intake.desc': '30 minutes to meet, explore fit, and define what helps now.',
    'pricing.note': 'Packages can combine personal training and life coaching, aligned with your goal.',
    'pricing.cta': 'Plan your intake',
    'contact.hero.title': 'Contact & booking with personal follow up',
    'contact.hero.subtitle': 'Request a conversation and take the first step to change with clear agreements.',
    'contact.info.title': 'Contact details and availability',
    'contact.info.desc': 'I reply within 24 hours on working days and gladly think with you.',
    'contact.info.emailLabel': 'Email address',
    'contact.info.phoneLabel': 'Phone number',
    'contact.info.locationLabel': 'Location and area',
    'contact.form.title': 'Book your intake and plan a moment',
    'contact.form.subtitle': 'Fill in the form and we will schedule a moment that fits your agenda.',
    'form.name': 'Full name',
    'form.email': 'Email address',
    'form.phone': 'Phone number',
    'form.topic': 'Focus area',
    'form.goal': 'Your goal or challenge',
    'form.time': 'Preferred times and days',
    'form.message': 'Extra info or context',
    'form.submit': 'Send your request',
    'form.statusIdle': 'We handle your request personally and with care.',
    'form.statusSending': 'One moment, your request is being sent securely.',
    'form.statusSuccess': 'Thank you, your request was sent and you will hear from me soon.',
    'form.statusError': 'Something went wrong. Please try again or email us directly.',
    'form.statusCaptcha': 'Please complete the captcha to send your request.',
    'form.statusConfigMissing': 'The form is temporarily unavailable. Please try again later or send an email.',
    'option.life': 'Life coaching track',
    'option.pt': 'Personal training sessions',
    'option.stress': 'Stress & burnout recovery',
    'option.assertive': 'Assertiveness training',
    'option.other': 'Other question or topic',
    'popup.title': 'Plan a free intake call?',
    'popup.subtitle': 'Request a quick introduction and discover your next step with clarity.',
    'popup.button': 'Yes, plan it',
    'footer.tagline': 'Warm, professional, and goal oriented coaching with attention to your pace.',
    'footer.servicesTitle': 'Services and tracks',
    'footer.linksTitle': 'Quick links',
    'footer.contactTitle': 'Contact and info',
    'footer.locationTitle': 'Location and area',
    'footer.rights': 'All rights reserved. TCoaching.'
  }
};

let currentLang = 'nl';
let currentTheme = 'light';
let captchaEnabled = false;
let captchaReady = false;
let publicConfigCache = null;
const intakeAutoPopupStorageKey = 'tcoaching.intakePopupAutoShown';

const getTranslation = (lang, key) => {
  if (!translations[lang] || !translations[lang][key]) {
    return null;
  }
  return translations[lang][key];
};

const cacheLanguageDefaults = () => {
  document.querySelectorAll('[data-lang-en]').forEach((el) => {
    if (!el.dataset.langNl) {
      el.dataset.langNl = el.innerHTML;
    }
  });

  document.querySelectorAll('[data-placeholder-en]').forEach((el) => {
    if (!el.dataset.placeholderNl) {
      el.dataset.placeholderNl = el.getAttribute('placeholder') || '';
    }
  });

  document.querySelectorAll('[data-content-en]').forEach((el) => {
    if (!el.dataset.contentNl) {
      el.dataset.contentNl = el.getAttribute('content') || '';
    }
  });
  document.querySelectorAll('[data-alt-en]').forEach((el) => {
    if (!el.dataset.altNl) {
      el.dataset.altNl = el.getAttribute('alt') || '';
    }
  });

  document.querySelectorAll('[data-aria-label-en]').forEach((el) => {
    if (!el.dataset.ariaLabelNl) {
      el.dataset.ariaLabelNl = el.getAttribute('aria-label') || '';
    }
  });

  document.querySelectorAll('[data-value-en]').forEach((el) => {
    if (!el.dataset.valueNl) {
      el.dataset.valueNl = el.getAttribute('value') || '';
    }
  });
};

const applyTranslations = (lang) => {
  document.querySelectorAll('[data-i18n]').forEach((el) => {
    const key = el.dataset.i18n;
    const value = getTranslation(lang, key);
    if (value) {
      el.textContent = value;
    }
  });

  document.querySelectorAll('[data-i18n-placeholder]').forEach((el) => {
    const key = el.dataset.i18nPlaceholder;
    const value = getTranslation(lang, key);
    if (value) {
      el.setAttribute('placeholder', value);
    }
  });

  document.querySelectorAll('[data-i18n-value]').forEach((el) => {
    const key = el.dataset.i18nValue;
    const value = getTranslation(lang, key);
    if (value) {
      el.setAttribute('value', value);
    }
  });
};

const applyContentLanguage = (lang) => {
  const english = lang === 'en';

  document.querySelectorAll('[data-lang-en]').forEach((el) => {
    const nextValue = english ? el.dataset.langEn : el.dataset.langNl;
    if (typeof nextValue === 'string') {
      el.innerHTML = nextValue;
    }
  });

  document.querySelectorAll('[data-placeholder-en]').forEach((el) => {
    const nextValue = english ? el.dataset.placeholderEn : el.dataset.placeholderNl;
    if (typeof nextValue === 'string') {
      el.setAttribute('placeholder', nextValue);
    }
  });

  document.querySelectorAll('[data-content-en]').forEach((el) => {
    const nextValue = english ? el.dataset.contentEn : el.dataset.contentNl;
    if (typeof nextValue === 'string') {
      el.setAttribute('content', nextValue);
    }
  });
  document.querySelectorAll('[data-alt-en]').forEach((el) => {
    const nextValue = english ? el.dataset.altEn : el.dataset.altNl;
    if (typeof nextValue === 'string') {
      el.setAttribute('alt', nextValue);
    }
  });

  document.querySelectorAll('[data-aria-label-en]').forEach((el) => {
    const nextValue = english ? el.dataset.ariaLabelEn : el.dataset.ariaLabelNl;
    if (typeof nextValue === 'string' && nextValue.length) {
      el.setAttribute('aria-label', nextValue);
    }
  });

  document.querySelectorAll('[data-value-en]').forEach((el) => {
    const nextValue = english ? el.dataset.valueEn : el.dataset.valueNl;
    if (typeof nextValue === 'string') {
      el.setAttribute('value', nextValue);
    }
  });
};

const getThemeToggleLabel = () => {
  return currentTheme === 'dark' ? '\u2600' : '\u263E';
};

const getThemeToggleAriaLabel = () => {
  if (currentLang === 'en') {
    return currentTheme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
  }
  return currentTheme === 'dark' ? 'Schakel naar lichte modus' : 'Schakel naar donkere modus';
};

const updateThemeColor = () => {
  const themeColor = currentTheme === 'dark' ? '#0c0c0d' : '#f6f1e8';
  document.querySelectorAll('meta[name="theme-color"]').forEach((meta) => {
    meta.setAttribute('content', themeColor);
  });
};

const updateThemeLabel = () => {
  const label = document.querySelector('[data-theme-label]');
  const button = document.querySelector('[data-theme-toggle]');
  if (!label) {
    return;
  }
  label.textContent = getThemeToggleLabel();
  if (button) {
    const ariaLabel = getThemeToggleAriaLabel();
    button.setAttribute('aria-label', ariaLabel);
    button.setAttribute('title', ariaLabel);
  }
};

const getMobileMenuLabel = (expanded) => {
  if (currentLang === 'en') {
    return expanded ? 'Close menu' : 'Menu';
  }
  return expanded ? 'Sluit menu' : 'Menu';
};

const syncMobileNavUi = () => {
  const button = document.querySelector('[data-nav-toggle]');
  const nav = document.querySelector('[data-nav]');
  if (!button || !nav) {
    return;
  }
  const expanded = nav.classList.contains('is-open');
  button.setAttribute('aria-expanded', String(expanded));
  button.setAttribute('aria-label', getMobileMenuLabel(expanded));
  button.setAttribute('title', getMobileMenuLabel(expanded));
};

const setLanguage = (lang) => {
  currentLang = lang;
  localStorage.setItem('siteLang', lang);
  document.documentElement.lang = lang;
  document.documentElement.dataset.lang = lang;
  applyContentLanguage(lang);
  applyTranslations(lang);
  updateThemeLabel();
  syncMobileNavUi();
  document.querySelectorAll('[data-lang-button]').forEach((btn) => {
    btn.classList.toggle('is-active', btn.dataset.lang === lang);
  });
};

const getInitialLang = () => {
  const stored = localStorage.getItem('siteLang');
  if (stored) {
    return stored;
  }
  if (navigator.language && navigator.language.toLowerCase().startsWith('en')) {
    return 'en';
  }
  return 'nl';
};

const setTheme = (theme) => {
  currentTheme = theme;
  localStorage.setItem('siteTheme', theme);
  document.documentElement.dataset.theme = theme;
  updateThemeLabel();
  updateThemeColor();
};

const getInitialTheme = () => {
  const stored = localStorage.getItem('siteTheme');
  if (stored) {
    return stored;
  }
  return 'dark';
};

const initThemeToggle = () => {
  const button = document.querySelector('[data-theme-toggle]');
  if (!button) {
    return;
  }
  button.addEventListener('click', () => {
    const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme);
  });
};

const initLangToggle = () => {
  document.querySelectorAll('[data-lang-button]').forEach((btn) => {
    btn.addEventListener('click', () => {
      setLanguage(btn.dataset.lang);
    });
  });
};

const initMobileNav = () => {
  const nav = document.querySelector('header nav');
  const headerInner = document.querySelector('.header-inner');
  const brand = headerInner ? headerInner.querySelector('.brand') : null;
  if (!nav || !headerInner || !brand) {
    return;
  }

  nav.id = nav.id || 'site-nav';
  nav.setAttribute('data-nav', '');

  let button = document.querySelector('[data-nav-toggle]');
  if (!button) {
    button = document.createElement('button');
    button.className = 'nav-toggle';
    button.type = 'button';
    button.setAttribute('data-nav-toggle', '');
    button.setAttribute('aria-expanded', 'false');
    button.setAttribute('aria-controls', nav.id);
    button.innerHTML = '<span></span><span></span><span></span>';
    brand.insertAdjacentElement('afterend', button);
  } else {
    button.setAttribute('aria-controls', nav.id);
  }

  const mobileQuery = window.matchMedia('(max-width: 820px)');
  const closeNav = () => {
    nav.classList.remove('is-open');
    document.body.classList.remove('nav-open');
    syncMobileNavUi();
  };

  button.addEventListener('click', () => {
    if (nav.classList.contains('is-open')) {
      closeNav();
      return;
    }
    nav.classList.add('is-open');
    document.body.classList.add('nav-open');
    syncMobileNavUi();
  });

  nav.querySelectorAll('a').forEach((link) => {
    link.addEventListener('click', () => {
      if (mobileQuery.matches) {
        closeNav();
      }
    });
  });

  const handleViewportChange = (event) => {
    if (!event.matches) {
      closeNav();
    }
  };

  if (mobileQuery.addEventListener) {
    mobileQuery.addEventListener('change', handleViewportChange);
  } else if (mobileQuery.addListener) {
    mobileQuery.addListener(handleViewportChange);
  }

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && nav.classList.contains('is-open')) {
      closeNav();
    }
  });

  syncMobileNavUi();
};

const initReveal = () => {
  const items = document.querySelectorAll('.reveal');
  if (!items.length) {
    return;
  }
  items.forEach((item, index) => {
    const delay = (index % 6) * 0.08;
    item.style.transitionDelay = `${delay}s`;
  });
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.2 }
  );
  items.forEach((item) => observer.observe(item));
};

const initPointerGlow = () => {
  const reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const finePointer = window.matchMedia && window.matchMedia('(hover: hover) and (pointer: fine)').matches;
  if (reduceMotion || !finePointer) {
    return;
  }

  document.body.classList.add('has-custom-cursor');
  const dot = document.createElement('div');
  dot.className = 'cursor-dot';
  dot.setAttribute('aria-hidden', 'true');

  const ring = document.createElement('div');
  ring.className = 'cursor-ring';
  ring.setAttribute('aria-hidden', 'true');

  document.body.append(dot, ring);

  let frameId = null;
  let nextX = -9999;
  let nextY = -9999;
  let ringX = -9999;
  let ringY = -9999;

  const render = () => {
    frameId = null;
    dot.style.transform = `translate3d(${nextX}px, ${nextY}px, 0)`;

    ringX += (nextX - ringX) * 0.2;
    ringY += (nextY - ringY) * 0.2;
    ring.style.transform = `translate3d(${ringX}px, ${ringY}px, 0)`;

    if (Math.abs(nextX - ringX) > 0.1 || Math.abs(nextY - ringY) > 0.1) {
      frameId = window.requestAnimationFrame(render);
    }
  };

  const queueRender = (event) => {
    nextX = Math.round(event.clientX);
    nextY = Math.round(event.clientY);
    if (ringX < -9000 || ringY < -9000) {
      ringX = nextX;
      ringY = nextY;
    }
    if (frameId === null) {
      frameId = window.requestAnimationFrame(render);
    }
  };

  const activateRing = (active) => {
    ring.classList.toggle('is-active', active);
  };

  window.addEventListener('pointermove', queueRender, { passive: true });
  document.querySelectorAll('a, button, input, textarea, select, summary, label').forEach((element) => {
    element.addEventListener('pointerenter', () => activateRing(true), { passive: true });
    element.addEventListener('pointerleave', () => activateRing(false), { passive: true });
  });
  window.addEventListener('pointerleave', () => {
    dot.style.transform = 'translate3d(-9999px, -9999px, 0)';
    ring.style.transform = 'translate3d(-9999px, -9999px, 0)';
  });
};

const initCardSpotlights = () => {
  const reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const finePointer = window.matchMedia && window.matchMedia('(hover: hover) and (pointer: fine)').matches;
  if (reduceMotion || !finePointer) {
    return;
  }

  const selector = [
    '.portrait-panel',
    '.video-frame',
    '.side-panel',
    '.story-card',
    '.trust-card',
    '.service-card',
    '.case-card',
    '.testimonial-card',
    '.quote-card',
    '.pricing-card',
    '.contact-panel',
    '.lead-card',
    '.booking-panel',
    '.article-card',
    '.metric-card',
    '.result-card',
    '.admin-card',
    '.admin-table-card',
    '.admin-metric'
  ].join(', ');

  document.querySelectorAll(selector).forEach((card) => {
    const setGlowPosition = (event) => {
      const rect = card.getBoundingClientRect();
      card.style.setProperty('--card-glow-x', `${event.clientX - rect.left}px`);
      card.style.setProperty('--card-glow-y', `${event.clientY - rect.top}px`);
    };

    card.addEventListener('pointerenter', setGlowPosition);
    card.addEventListener('pointermove', setGlowPosition);
  });
};

const initPopup = () => {
  const overlay = document.querySelector('[data-intake-modal]');
  if (!overlay) {
    return;
  }
  const closeButtons = overlay.querySelectorAll('[data-close-intake]');
  const openButtons = document.querySelectorAll('[data-open-intake]');
  let autoOpened = false;
  const hasSeenAutoPopup = () => localStorage.getItem(intakeAutoPopupStorageKey) === 'true';

  const closeModal = () => {
    overlay.classList.remove('is-visible');
  };

  const openModal = (source = 'manual') => {
    overlay.classList.add('is-visible');
    autoOpened = true;
    if (source === 'auto') {
      localStorage.setItem(intakeAutoPopupStorageKey, 'true');
    }
  };

  closeButtons.forEach((btn) => btn.addEventListener('click', closeModal));
  overlay.addEventListener('click', (event) => {
    if (event.target === overlay) {
      closeModal();
    }
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      closeModal();
    }
  });

  openButtons.forEach((btn) =>
    btn.addEventListener('click', () => {
      openModal('manual');
    })
  );

  setTimeout(() => {
    if (!autoOpened && !hasSeenAutoPopup()) {
      openModal('auto');
    }
  }, 8000);
};

const initHeaderState = () => {
  const header = document.querySelector('header');
  if (!header) {
    return;
  }

  const mobileLayout = window.matchMedia && window.matchMedia('(max-width: 820px)');
  let ticking = false;

  const syncHeaderState = () => {
    ticking = false;
    const shouldCondense = !(mobileLayout && mobileLayout.matches) && window.scrollY > 56;
    header.classList.toggle('is-condensed', shouldCondense);
  };

  const queueSync = () => {
    if (ticking) {
      return;
    }
    ticking = true;
    window.requestAnimationFrame(syncHeaderState);
  };

  syncHeaderState();
  window.addEventListener('scroll', queueSync, { passive: true });
  if (mobileLayout) {
    if (typeof mobileLayout.addEventListener === 'function') {
      mobileLayout.addEventListener('change', syncHeaderState);
    } else if (typeof mobileLayout.addListener === 'function') {
      mobileLayout.addListener(syncHeaderState);
    }
  }
};

const trimValue = (value) => {
  if (!value) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length ? trimmed : null;
};

const limitValue = (value, max) => {
  if (!value) {
    return null;
  }
  return value.length > max ? value.slice(0, max) : value;
};

const getStaticModeMessage = () => {
  if (currentLang === 'en') {
    return 'This shared static version cannot send requests directly. Use the live app version with backend support.';
  }
  return 'Deze gedeelde statische versie kan geen aanvragen rechtstreeks versturen. Gebruik de live appversie met backend.';
};

const buildTrackingPayload = (overrides = {}) => {
  const path = limitValue(overrides.path || window.location.pathname || '/', 200);
  if (!path) {
    return null;
  }

  return {
    path,
    title: limitValue(overrides.title ?? document.title, 200),
    referrer: limitValue(overrides.referrer ?? document.referrer, 255),
    lang: limitValue(overrides.lang ?? currentLang, 10),
    eventType: limitValue(overrides.eventType, 40),
    eventName: limitValue(overrides.eventName, 80),
    eventValue: limitValue(overrides.eventValue, 255)
  };
};

const sendJson = async (url, payload, keepalive = false) => {
  if (!url) {
    throw new Error('Missing endpoint');
  }
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload),
    keepalive
  });
  if (!response.ok) {
    throw new Error('Request failed');
  }
};

const dispatchTrackingPayload = (payload, { keepalive = true, immediate = false } = {}) => {
  if (!payload || !apiConfig.enabled || !apiConfig.trackEndpoint) {
    return;
  }

  const sendTracking = () => {
    sendJson(apiConfig.trackEndpoint, payload, keepalive).catch(() => {});
  };

  if (immediate) {
    sendTracking();
    return;
  }

  if ('requestIdleCallback' in window) {
    window.requestIdleCallback(sendTracking, { timeout: 1500 });
    return;
  }

  window.setTimeout(sendTracking, 250);
};

const fetchPublicConfig = async () => {
  if (publicConfigCache) {
    return publicConfigCache;
  }
  if (!apiConfig.enabled || !apiConfig.publicConfigEndpoint) {
    return null;
  }
  try {
    const response = await fetch(apiConfig.publicConfigEndpoint, { cache: 'no-store' });
    if (!response.ok) {
      return null;
    }
    publicConfigCache = await response.json();
    if (publicConfigCache && publicConfigCache.bookingUrl && !apiConfig.bookingUrl) {
      apiConfig.bookingUrl = publicConfigCache.bookingUrl;
    }
    return publicConfigCache;
  } catch (error) {
    return null;
  }
};

const loadScript = (src) =>
  new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve();
      return;
    }
    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Script load failed'));
    document.head.appendChild(script);
  });

const initCaptcha = async () => {
  if (!apiConfig.enabled) {
    return;
  }

  const config = await fetchPublicConfig();
  if (!config || !config.captchaEnabled) {
    return;
  }
  captchaEnabled = true;

  if (!config.captchaSiteKey) {
    return;
  }

  try {
    await loadScript('https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit');
  } catch (error) {
    return;
  }

  if (!window.turnstile) {
    return;
  }

  captchaReady = true;

  document.querySelectorAll('[data-captcha-widget]').forEach((widget) => {
    const form = widget.closest('form');
    const tokenField = form ? form.querySelector('[data-captcha-token]') : null;
    const widgetId = window.turnstile.render(widget, {
      sitekey: config.captchaSiteKey,
      callback: (token) => {
        if (tokenField) {
          tokenField.value = token;
        }
      },
      'error-callback': () => {
        if (tokenField) {
          tokenField.value = '';
        }
      },
      'expired-callback': () => {
        if (tokenField) {
          tokenField.value = '';
        }
      }
    });
    widget.dataset.captchaWidgetId = String(widgetId);
  });
};

const initBooking = async () => {
  const config = await fetchPublicConfig();
  const bookingUrl = trimValue((config && config.bookingUrl) || apiConfig.bookingUrl);
  const directLink = document.querySelector('[data-booking-link]:not([data-booking-fallback])');
  const fallbackLink = document.querySelector('[data-booking-link][data-booking-fallback]');
  const status = document.querySelector('[data-booking-status]');
  const embed = document.querySelector('[data-booking-embed]');
  const embedShell = document.querySelector('[data-booking-embed-shell]');
  const placeholder = document.querySelector('[data-booking-placeholder]');

  if (!bookingUrl) {
    if (directLink) {
      directLink.hidden = true;
    }
    return;
  }

  if (directLink) {
    directLink.hidden = false;
    directLink.href = bookingUrl;
    directLink.target = '_blank';
    directLink.rel = 'noreferrer';
  }

  if (fallbackLink) {
    fallbackLink.textContent = currentLang === 'en' ? 'Request via form' : 'Vraag aan via formulier';
  }

  if (status) {
    status.textContent = currentLang === 'en'
      ? 'A direct booking tool is active. You can open it immediately or still use the form below for a personal proposal.'
      : 'Er is een directe boekingstool actief. Je kan die meteen openen of alsnog het formulier hieronder gebruiken voor een persoonlijk voorstel.';
  }

  if (embed && embedShell) {
    embed.src = bookingUrl;
    embedShell.hidden = false;
  }
  if (placeholder) {
    placeholder.hidden = true;
  }
};

const resetCaptcha = (form) => {
  const tokenField = form.querySelector('[data-captcha-token]');
  if (tokenField) {
    tokenField.value = '';
  }
  const widget = form.querySelector('[data-captcha-widget]');
  if (widget && widget.dataset.captchaWidgetId && window.turnstile) {
    window.turnstile.reset(widget.dataset.captchaWidgetId);
  }
};

const buildContactPayload = (form) => {
  const formData = new FormData(form);
  const payload = {};

  formData.forEach((value, key) => {
    const normalized = trimValue(String(value));
    if (normalized) {
      payload[key] = normalized;
    }
  });

  payload.lang = currentLang;
  return payload;
};

const initTrackedClicks = () => {
  document.querySelectorAll('[data-track-click], [data-open-intake]').forEach((element) => {
    element.addEventListener('click', () => {
      const eventName = trimValue(element.dataset.trackClick)
        || (element.hasAttribute('data-open-intake') ? 'intake_open' : 'cta_click');
      const eventValue = limitValue(
        trimValue(element.dataset.trackValue)
          || trimValue(element.textContent)
          || element.getAttribute('href'),
        255
      );

      dispatchTrackingPayload(
        buildTrackingPayload({
          eventType: 'cta_click',
          eventName,
          eventValue
        }),
        { keepalive: true, immediate: true }
      );
    });
  });
};

const wireForms = () => {
  const forms = document.querySelectorAll('[data-contact-form]');
  if (!forms.length) {
    return;
  }

  forms.forEach((form) => {
    const trackingName = trimValue(form.dataset.contactForm)
      || trimValue(form.querySelector('input[name="page"]')?.value)
      || 'contact-form';
    const markFormStarted = () => {
      if (form.dataset.formStarted === 'true') {
        return;
      }
      form.dataset.formStarted = 'true';
      dispatchTrackingPayload(
        buildTrackingPayload({
          eventType: 'form_start',
          eventName: trackingName,
          eventValue: trimValue(form.querySelector('select[name="topic"]')?.value)
        }),
        { keepalive: true, immediate: true }
      );
    };

    form.querySelectorAll('input, select, textarea').forEach((field) => {
      field.addEventListener('focus', markFormStarted, { passive: true });
      field.addEventListener('input', markFormStarted, { passive: true });
    });

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const status = form.querySelector('[data-form-status]');
      const setStatus = (key) => {
        if (!status) {
          return;
        }
        const value = getTranslation(currentLang, key);
        status.textContent = value || '';
      };

      setStatus('form.statusSending');

      if (captchaEnabled) {
        if (!captchaReady) {
          setStatus('form.statusConfigMissing');
          return;
        }
        const tokenField = form.querySelector('[data-captcha-token]');
        const token = tokenField ? trimValue(tokenField.value) : null;
        if (!token) {
          setStatus('form.statusCaptcha');
          resetCaptcha(form);
          return;
        }
      }

      const payload = buildContactPayload(form);
      if (payload.website) {
        setStatus('form.statusSuccess');
        form.reset();
        if (captchaEnabled) {
          resetCaptcha(form);
        }
        return;
      }

      if (!apiConfig.enabled) {
        setStatus(getStaticModeMessage());
        return;
      }

      try {
        await sendJson(apiConfig.contactEndpoint, payload);
        setStatus('form.statusSuccess');
        dispatchTrackingPayload(
          buildTrackingPayload({
            eventType: 'form_submit_success',
            eventName: trackingName,
            eventValue: payload.topic || payload.page || null
          }),
          { keepalive: true, immediate: true }
        );
        form.reset();
        delete form.dataset.formStarted;
        if (captchaEnabled) {
          resetCaptcha(form);
        }
      } catch (error) {
        setStatus('form.statusError');
        if (captchaEnabled) {
          resetCaptcha(form);
        }
      }
    });
  });
};

  const initStaticModeHints = () => {
    if (apiConfig.enabled) {
      return;
    }

    document.documentElement.dataset.siteMode = apiConfig.mode;
    document.querySelectorAll('[data-contact-form]').forEach((form) => {
      form.dataset.staticMode = 'true';
      const submitButton = form.querySelector('button[type="submit"], input[type="submit"]');
      if (submitButton) {
        submitButton.disabled = true;
        submitButton.setAttribute('aria-disabled', 'true');
        submitButton.title = getStaticModeMessage();
      }
    });
    document.querySelectorAll('[data-form-status]').forEach((element) => {
      element.textContent = getStaticModeMessage();
    });
    document.querySelectorAll('[data-captcha-widget]').forEach((element) => {
        element.remove();
    });
};

const trackPageView = () => {
  const payload = buildTrackingPayload();
  if (!payload) {
    return;
  }

  try {
    const storageKey = `pageview:${payload.path}`;
    const previous = Number(sessionStorage.getItem(storageKey) || 0);
    const now = Date.now();
    if (previous && now - previous < trackingWindowMs) {
      return;
    }
    sessionStorage.setItem(storageKey, String(now));
  } catch (error) {
    // Ignore storage access failures and continue with best-effort tracking.
  }

  dispatchTrackingPayload(payload, { keepalive: true, immediate: false });
};
document.addEventListener('DOMContentLoaded', async () => {
  document.body.classList.add('is-loaded');
  cacheLanguageDefaults();
  currentLang = getInitialLang();
  currentTheme = getInitialTheme();
  setTheme(currentTheme);
  setLanguage(currentLang);
  initMobileNav();
  initHeaderState();
  initThemeToggle();
  initLangToggle();
  initReveal();
  initPointerGlow();
  initCardSpotlights();
  initPopup();
  initTrackedClicks();
  initStaticModeHints();
  await initBooking();
  await initCaptcha();
  trackPageView();
  wireForms();
});



