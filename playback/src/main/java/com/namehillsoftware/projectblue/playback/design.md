Engine ->
    - PlayableFile
    - Preparation ->
        - Queue ->
            - PreparedPlayableFileQueue
        - PlayableFilePreparationSource
        - PreparedPlayableFile
Playlist ->
    -> Play: PlayingPlaylist -> Future state
    -> Pause: PausedPlaylist -> Future state
    -> Resume: PlayingPlaylist -> Future state
    -> ChangePosition: 
    -> Add
    -> Remove
    -> Insert
