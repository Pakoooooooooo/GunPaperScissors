# Instructions du projet

## Stack technique & versions
- **Langage** : Kotlin (idiomatique, immutabilité par défaut, coroutines & Flow)
- **UI** : Jetpack Compose exclusivement (pas de XML/ViewBinding sauf mention explicite)
- **Architecture** : MVVM + Clean Architecture (couches `presentation`, `domain`, `data`) (les classes de Model, de View et de ViewModel doivent être écrites dans des fichiers séparés)
- **Injection de dépendances** : Koin (ou Hilt selon ton setup)
- **Asynchronisme** : `StateFlow` et `SharedFlow` pour l'exposition d'état, pas de LiveData

## Règles d'architecture
- La couche `domain` ne doit dépendre d'aucun composant framework Android.
- Tout état UI doit être représenté par une `data class` ou `sealed interface` immuable (`UiState`).
- Les UseCases n'exposent qu'une seule fonction publique (`operator fun invoke(...)`).
- Le repository gère la source unique de vérité (`Single Source of Truth`).

## Jetpack Compose
- Nommer les composants avec un verbe/nom clair en PascalCase.
- Toujours passer un `modifier: Modifier = Modifier` en premier paramètre optionnel.
- Séparer les composants avec état (*stateful*) des composants d'affichage pur (*stateless*).
- Ne jamais instancier de ViewModel directement dans un sous-composant réutilisable (préférer le passage d'événements par lambdas).

## Style & formatage
- Préférer `when` exhaustif sur les `sealed class/interface`.
- Pas de `lateinit var` si une alternative immuable ou déléguée existe.
- Commentaires minimaux : le code doit s'auto-documenter.
- Rédiger les messages d'erreur et logs en anglais.
- Retire tous les imports inutilisés