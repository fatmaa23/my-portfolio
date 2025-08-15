// We are explicitly telling Jenkins to load the library named 'my-portfolio'.
// The @main part means use the 'main' branch of that library.
@Library('my-portfolio@main') _

// Now we can call the function from the library
buildAndDeploy(
    gitUrl: 'https://github.com/fatmaa23/my-portfolio.git',
    gitBranch: 'main',
    dockerhubUser: 'fatmaa23', // <-- Make sure to replace this!
    imageRepo: 'my-portfolio',
    githubRepo: 'fatmaa23/my-portfolio'
)