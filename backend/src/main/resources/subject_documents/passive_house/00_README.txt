PASSIVE HOUSE & SUSTAINABLE ARCHITECTURE — RAG KNOWLEDGE BASE
Version: 1.0 (2026-07)
Language: English
Purpose: Retrieval-augmented generation knowledge corpus for an educational subject-matter agent.

SCOPE
This corpus explains Passive House principles, high-performance envelopes, ventilation, comfort, climate-responsive design, sustainable materials, water, renewable energy, commissioning, retrofit, operation, and common design trade-offs. It is intended for conceptual guidance, preliminary reasoning, question answering, and design reviews.

LIMITATIONS
This is not a substitute for local building codes, fire regulations, structural engineering, moisture analysis, energy modelling, manufacturer instructions, or certification review. Numerical criteria may differ by certification system, climate, building use, edition, and jurisdiction. For a real project, verify current official criteria and local law.

DIRECTORY MAP
01_fundamentals        Core concepts, terminology, performance metrics and design process.
02_building_envelope   Insulation, airtightness, windows, thermal bridges and moisture.
03_building_services   Ventilation, heating, cooling, hot water, controls and renewables.
04_climate_and_design  Climate-responsive form, solar design, shading, daylight and comfort.
05_sustainability      Embodied carbon, materials, circularity, water, ecology and resilience.
06_delivery_and_use    Modelling, construction QA, commissioning, retrofit and operation.
07_reference           Glossary, checklists, calculations, misconceptions and source notes.

RECOMMENDED INGESTION
Prefer structure-aware splitting by file, heading and paragraph. Preserve filename, folder, title, section heading, version, and tags as metadata. Do not concatenate the entire corpus before splitting.

DEFAULT CHUNKING
Recommended starting point for compact English embedding models:
- Chunk size: 500–700 tokens
- Overlap: 70–110 tokens
- Good default: 600 tokens with 90-token overlap

For short definition/reference files, use 300–450 tokens with 40–70 overlap. For procedural files and long explanations, use 650–850 tokens with 80–130 overlap. Avoid chunks below about 150 tokens unless they represent a self-contained definition.

RETRIEVAL
Start with top_k=5 or 6. For broad design questions, retrieve 8–10 candidates and rerank to 4–6. Use hybrid retrieval if available: semantic vectors plus lexical/BM25. Add a modest score threshold rather than forcing irrelevant context. Retrieve adjacent chunks when a selected chunk begins or ends mid-topic.

ANSWERING POLICY FOR THE AGENT
1. Distinguish facts, rules of thumb, examples, and project-specific assumptions.
2. State climate and building-use dependencies.
3. Never claim certification without verified calculations and documentation.
4. Recommend specialist review for fire, structure, acoustics, radon, hazardous materials, and complex moisture risks.
5. Cite source file names or metadata in answers when possible.
