package com.example.util

import com.example.data.ComponentEntity
import com.example.data.ProjectEntity

object WebCompiler {

    /**
     * Compiles a project and its component entities into a clean, complete HTML package.
     */
    fun compileProjectToHtml(project: ProjectEntity, components: List<ComponentEntity>): String {
        val builder = StringBuilder()
        builder.append("""
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${project.name}</title>
                <!-- Tailwind CSS for modern layouts -->
                <script src="https://cdn.tailwindcss.com"></script>
                <!-- Tailwind configuration in case customization is needed -->
                <script>
                    tailwind.config = {
                        theme: {
                            extend: {
                                colors: {
                                    primary: '#3b82f6',
                                    secondary: '#9333ea',
                                }
                            }
                        }
                    }
                </script>
                <style>
                    body {
                        font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        margin: 0;
                        padding: 0;
                    }
                    .preview-glass {
                        background: rgba(255, 255, 255, 0.45);
                        backdrop-filter: blur(14px);
                        -webkit-backdrop-filter: blur(14px);
                        border: 1px solid rgba(255, 255, 255, 0.25);
                    }
                    .dark-gradient {
                        background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
                    }
                    /* Injected Custom User CSS */
                    ${project.customCss}
                </style>
            </head>
            <body class="min-h-screen flex flex-col transition-colors duration-300">
        """.trimIndent())

        // Find root level components (parentId is null or not found in current components)
        val parentIds = components.map { it.id }.toSet()
        val roots = components.filter { it.parentId == null || !parentIds.contains(it.parentId) }
            .sortedBy { it.orderIndex }
        
        for (root in roots) {
            builder.append(renderComponent(root, components))
        }

        builder.append("""
                <!-- Injected Custom User JS -->
                <script>
                    ${project.customJs}
                </script>
            </body>
            </html>
        """.trimIndent())
        
        return builder.toString()
    }

    private fun renderComponent(comp: ComponentEntity, allComps: List<ComponentEntity>): String {
        val children = allComps.filter { it.parentId == comp.id }.sortedBy { it.orderIndex }
        
        // Background and style tokens resolution
        val bgClass = when (comp.bgStyle) {
            "white" -> "bg-white shadow-md border border-slate-100"
            "dark" -> "bg-slate-900 border border-slate-800 text-white"
            "primary" -> "bg-blue-600 shadow-blue-500/20 text-white"
            "secondary" -> "bg-purple-600 shadow-purple-500/20 text-white"
            "accent" -> "bg-amber-500 text-slate-900"
            "glass" -> "preview-glass shadow-lg"
            "transparent" -> ""
            else -> ""
        }
        
        val textColorClass = when (comp.textColorStyle) {
            "dark" -> "text-slate-900"
            "light" -> "text-slate-100"
            "primary" -> "text-blue-600 font-semibold"
            "accent" -> "text-amber-500 font-semibold"
            else -> ""
        }
        
        val paddingClass = when (comp.paddingStyle) {
            "none" -> "p-0"
            "small" -> "p-2 md:p-3"
            "medium" -> "p-4 md:p-6"
            "large" -> "p-8 md:p-12"
            else -> "p-4"
        }

        val marginClass = when (comp.marginStyle) {
            "none" -> "m-0"
            "small" -> "my-2"
            "medium" -> "my-4 md:my-6"
            "large" -> "my-6 md:my-10"
            else -> "m-0"
        }

        val roundedClass = when (comp.roundedStyle) {
            "none" -> "rounded-none"
            "small" -> "rounded-md"
            "medium" -> "rounded-xl"
            "large" -> "rounded-2xl"
            "full" -> "rounded-full"
            else -> "rounded-none"
        }

        val widthClass = when (comp.widthStyle) {
            "auto" -> "w-auto self-center"
            "half" -> "w-full md:w-1/2 mx-auto"
            "full" -> "w-full"
            else -> "w-full"
        }

        val alignClass = when (comp.alignment) {
            "left" -> "text-left"
            "center" -> "text-center"
            "right" -> "text-right"
            else -> ""
        }

        val flexClass = if (comp.isFlexRow) "flex flex-row items-center justify-between gap-4" else "flex flex-col gap-4"

        // Put it together
        val stylingClasses = listOf(bgClass, textColorClass, paddingClass, marginClass, roundedClass, widthClass, alignClass)
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        val contentBuilder = StringBuilder()
        if (children.isNotEmpty()) {
            for (child in children) {
                contentBuilder.append(renderComponent(child, allComps))
            }
        } else {
            // Check if text is raw html blocks (like those imported from AI or designed by programmers)
            val isRawHtml = comp.text.trim().startsWith("<") && comp.text.trim().endsWith(">")
            if (isRawHtml) {
                contentBuilder.append(comp.text)
            } else {
                contentBuilder.append(comp.text)
            }
        }

        val idAttr = "id=\"${comp.id}\""

        return when (comp.type) {
            "container" -> {
                "<div $idAttr class=\"$stylingClasses $flexClass\">$contentBuilder</div>"
            }
            "heading" -> {
                val isRaw = comp.text.trim().startsWith("<") && comp.text.trim().endsWith(">")
                if (isRaw) comp.text else "<h1 $idAttr class=\"text-3xl md:text-4xl font-extrabold tracking-tight $stylingClasses\">$contentBuilder</h1>"
            }
            "paragraph" -> {
                val isRaw = comp.text.trim().startsWith("<") && comp.text.trim().endsWith(">")
                if (isRaw) comp.text else "<p $idAttr class=\"text-base leading-relaxed text-slate-600 dark:text-slate-300 $stylingClasses\">$contentBuilder</p>"
            }
            "button" -> {
                val btnRounded = if (comp.roundedStyle == "none") "rounded-lg" else roundedClass
                "<button $idAttr class=\"px-6 py-3 font-bold transition-all transform hover:scale-[1.02] active:scale-[0.98] outline-none duration-150 $bgClass $textColorClass $btnRounded $widthClass\">${comp.text}</button>"
            }
            "link" -> {
                "<a $idAttr href=\"${comp.href.ifBlank { "#" }}\" class=\"text-blue-500 hover:text-blue-600 hover:underline transition-colors font-medium $stylingClasses\">${comp.text}</a>"
            }
            "image" -> {
                "<img $idAttr src=\"${comp.imageUrl}\" alt=\"Web Sketch Image\" class=\"shadow-sm max-h-[400px] object-cover $stylingClasses\" />"
            }
            "input" -> {
                "<input $idAttr type=\"text\" placeholder=\"${comp.placeholder.ifBlank { "أدخل نص..." }}\" class=\"border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-4 py-3 text-slate-800 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all $stylingClasses\" />"
            }
            "divider" -> {
                "<div $idAttr class=\"w-full flex justify-center py-2\"><hr class=\"border-t border-slate-200 dark:border-slate-800 w-[95%]\" /></div>"
            }
            "card_hero" -> {
                """
                <section $idAttr class="bg-gradient-to-r from-blue-600 to-indigo-800 text-white rounded-3xl p-8 md:p-14 my-6 text-center shadow-xl flex flex-col gap-5 items-center justify-center">
                    <h2 class="text-3xl md:text-5xl font-black">${comp.text}</h2>
                    <p class="text-slate-100 text-base md:text-lg opacity-90 max-w-2xl leading-relaxed">
                        برمجة صفحات الويب أصبحت أسهل بكثير بفضل هذا التطبيق المتطور! ابدأ الآن بتصميم صفحات تفاعلية كاملة وحفظها محلياً بأعلى كود متاح.
                    </p>
                    <div class="flex flex-wrap justify-center gap-4 mt-2">
                        <button class="bg-white text-blue-900 px-6 py-3 rounded-xl font-bold shadow hover:bg-slate-100 transition active:scale-95 duration-150">تصفح الخدمات</button>
                        <button class="border border-white/60 bg-white/10 text-white px-6 py-3 rounded-xl font-medium hover:bg-white/20 transition active:scale-95 duration-150">عرض المزيد</button>
                    </div>
                </section>
                """.trimIndent()
            }
            "card_pricing" -> {
                """
                <div $idAttr class="grid md:grid-cols-3 gap-6 my-6 w-full text-slate-900">
                    <!-- Basic -->
                    <div class="bg-white border border-slate-100 rounded-2xl p-6 shadow-sm hover:shadow-md transition flex flex-col justify-between text-center">
                        <div>
                            <h3 class="text-xl font-bold text-slate-800">الباقة الأساسية</h3>
                            <p class="text-slate-500 text-sm mt-1">للمطور المبتدئ</p>
                            <div class="text-3xl font-black text-slate-900 mt-4">0$</div>
                        </div>
                        <ul class="text-slate-600 space-y-2.5 my-6 text-right text-sm">
                            <li>✔️ تصميم صفحة واحدة كاملة</li>
                            <li>✔️ 8 مكونات سحب وإفلات</li>
                            <li>✔️ تصدير كود HTML مجاناً</li>
                        </ul>
                        <button class="bg-slate-800 hover:bg-slate-900 text-white w-full py-2.5 rounded-xl font-bold transition">ابدأ الآن</button>
                    </div>
                    <!-- Pro -->
                    <div class="bg-slate-900 text-white border-2 border-blue-500 rounded-2xl p-6 shadow-lg hover:shadow-xl transition flex flex-col justify-between text-center relative transform scale-105">
                        <span class="absolute -top-3 left-1/2 -translate-x-1/2 bg-blue-500 text-white text-[11px] px-3.5 py-1 rounded-full font-bold">الأكثر اختياراً</span>
                        <div>
                            <h3 class="text-xl font-bold mt-2">المحترف Pro</h3>
                            <p class="text-slate-300 text-sm mt-1">لأرقى تصاميم الويب</p>
                            <div class="text-3xl font-black mt-4">19$<span class="text-xs font-normal">/شهرياً</span></div>
                        </div>
                        <ul class="text-slate-200 space-y-2.5 my-6 text-right text-sm">
                            <li>✔️ تصميم صفحات متعددة بلا قيود</li>
                            <li>✔️ كافة مكونات السحب المعقدة</li>
                            <li>✔️ تخصيص CSS & Javascript</li>
                            <li>✔️ مساعد ذكي AI غير محدود</li>
                        </ul>
                        <button class="bg-blue-500 hover:bg-blue-600 text-white w-full py-2.5 rounded-xl font-bold transition">اشترك بمهرجان الخصم</button>
                    </div>
                    <!-- Enterprise -->
                    <div class="bg-white border border-slate-100 rounded-2xl p-6 shadow-sm hover:shadow-md transition flex flex-col justify-between text-center">
                        <div>
                            <h3 class="text-xl font-bold text-slate-800">باقة الأعمال</h3>
                            <p class="text-slate-500 text-sm mt-1">للفرق والشركات</p>
                            <div class="text-3xl font-black text-slate-900 mt-4">49$</div>
                        </div>
                        <ul class="text-slate-600 space-y-2.5 my-6 text-right text-sm">
                            <li>✔️ كل شيء في المحترف Pro</li>
                            <li>✔️ استضافة ونشر بلمحة واحدة</li>
                            <li>✔️ تصدير ملفات مضغوطة جاهزة</li>
                        </ul>
                        <button class="bg-slate-800 hover:bg-slate-900 text-white w-full py-2.5 rounded-xl font-bold transition">اتصل بفريق المبيعات</button>
                    </div>
                </div>
                """.trimIndent()
            }
            else -> {
                "<div $idAttr class=\"$stylingClasses\">$contentBuilder</div>"
            }
        }
    }
}
