/** @type {import('tailwindcss').Config} */
export default {
    content: [
        './resources/**/*.blade.php',
        './resources/**/*.js',
    ],
    theme: {
        extend: {
            fontFamily: {
                switzer: ['Switzer-Extrabold', 'sans-serif'],
            },
        },
    },
    plugins: [],
};