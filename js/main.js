document.addEventListener('DOMContentLoaded', () => {
    // Mobile Menu Toggle
    const mobileMenuBtn = document.querySelector('.mobile-menu-btn');
    const navLinks = document.querySelector('.nav-links');

    if (mobileMenuBtn) {
        mobileMenuBtn.addEventListener('click', () => {
            navLinks.classList.toggle('active');
            const icon = mobileMenuBtn.querySelector('i');
            const isActive = navLinks.classList.contains('active');

            // Toggle Body Scroll
            if (isActive) {
                document.body.style.overflow = 'hidden';
            } else {
                document.body.style.overflow = '';
            }

            if (icon) {
                if (isActive) {
                    icon.classList.remove('ri-menu-line');
                    icon.classList.add('ri-close-line');
                } else {
                    icon.classList.remove('ri-close-line');
                    icon.classList.add('ri-menu-line');
                }
            }
        });
    }

    // Typing Animation Logic
    const demoText = document.getElementById('demo-text');
    const demoStatus = document.getElementById('demo-status');
    const demoIndicator = document.getElementById('demo-indicator');

    if (demoText) {
        const scenarios = [
            // @aido
            { query: "What is the capital of France? @aido", result: "Paris." },
            { query: "Who wrote Hamlet? @aido", result: "William Shakespeare." },
            { query: "Boiling point of water? @aido", result: "100°C (212°F)." },

            // @fixg
            { query: "This sentence are wrong @fixg", result: "This sentence is wrong." },
            { query: "He dont know nothing @fixg", result: "He doesn't know anything." },
            { query: "I going to school yesterday @fixg", result: "I went to school yesterday." },

            // @summ
            { query: "The project was delayed due to unforeseen weather conditions and lack of resources. @summ", result: "Project delayed by weather and resource shortage." },
            { query: "Artificial Intelligence is changing the world by automating tasks and improving efficiency. @summ", result: "AI transforms the world through automation and efficiency." },

            // @polite
            { query: "I need this now @polite", result: "Could you please provide this at your earliest convenience?" },
            { query: "Send me the file @polite", result: "Could you kindly send me the file?" },
            { query: "You are wrong @polite", result: "I believe there might be a misunderstanding." },

            // @casual
            { query: "Dear Sir, I am writing to inform you @casual", result: "Hey, just wanted to let you know" },
            { query: "I apologize for the delay @casual", result: "Sorry for the wait!" },

            // @expand
            { query: "AI is useful @expand", result: "AI is useful because it automates repetitive tasks, analyzes data quickly, and assists in complex decision-making." },
            { query: "Exercise is good @expand", result: "Exercise is good for maintaining physical health, improving mental well-being, and boosting energy levels." },

            // @bullet
            { query: "Apples, Bananas, Oranges @bullet", result: "• Apples\n• Bananas\n• Oranges" },
            { query: "Monday, Tuesday, Wednesday @bullet", result: "• Monday\n• Tuesday\n• Wednesday" },

            // @improve
            { query: "Make this better @improve", result: "Enhance the quality of this text." },
            { query: "The movie was good @improve", result: "The movie was compelling and enjoyable." },

            // @rephrase
            { query: "Hello world @rephrase", result: "Greetings, world." },
            { query: "I am busy @rephrase", result: "I am currently occupied." },

            // @emoji
            { query: "Great news @emoji", result: "Great news! 🎉 🥳" },
            { query: "I love pizza @emoji", result: "I love pizza! 🍕 ❤️" },

            // @formal
            { query: "Hey what's up @formal", result: "Greetings, how are you?" },
            { query: "See ya later @formal", result: "I look forward to speaking with you later." },

            // @funny
            { query: "I am tired @funny", result: "I am running on 1% battery and hope." },
            { query: "It is raining @funny", result: "The sky is leaking again." },

            // @prompt
            { query: "write email for leave @prompt", result: "Subject: Leave Request\n\nDear Manager,\nI would like to request leave for [reason] from [start date] to [end date]..." }
        ];

        // Fisher-Yates Shuffle
        function shuffleArray(array) {
            for (let i = array.length - 1; i > 0; i--) {
                const j = Math.floor(Math.random() * (i + 1));
                [array[i], array[j]] = [array[j], array[i]];
            }
            return array;
        }

        let shuffledScenarios = shuffleArray([...scenarios]);
        let currentScenarioIndex = 0;

        async function typeText(text) {
            demoText.textContent = '';
            // Show Typing Status
            if (demoStatus) demoStatus.textContent = "Typing...";
            if (demoIndicator) demoIndicator.style.backgroundColor = "var(--primary)";
            if (demoIndicator) demoIndicator.style.boxShadow = "0 0 10px var(--primary)";

            for (let i = 0; i < text.length; i++) {
                demoText.textContent += text.charAt(i);
                await new Promise(r => setTimeout(r, 50 + Math.random() * 50));
            }
        }

        async function runDemo() {
            while (true) {
                // Reshuffle if we've gone through all
                if (currentScenarioIndex >= shuffledScenarios.length) {
                    shuffledScenarios = shuffleArray([...scenarios]);
                    currentScenarioIndex = 0;
                }

                const scenario = shuffledScenarios[currentScenarioIndex];

                // 1. Type Query
                await typeText(scenario.query);

                // 2. Pause before processing
                await new Promise(r => setTimeout(r, 500));

                // 3. Simulate "Aido Magic" / Processing
                if (demoStatus) demoStatus.textContent = "Aido Magic ✨";
                if (demoIndicator) demoIndicator.style.backgroundColor = "var(--secondary)";
                if (demoIndicator) demoIndicator.style.boxShadow = "0 0 15px var(--secondary)";

                await new Promise(r => setTimeout(r, 800));

                // 4. Show Result
                demoText.textContent = scenario.result;

                // 5. Pause reading time
                await new Promise(r => setTimeout(r, 2500));

                // Next scenario
                currentScenarioIndex++;
            }
        }

        runDemo();
    }
});
