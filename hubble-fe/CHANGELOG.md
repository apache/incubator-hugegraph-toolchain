## 1.6.0 (2020-08-14)

#### :rocket: New Feature

- Add analyzing algorithm tab in data-analyze module, only support shortest-path algorithm for now

<br />

## 1.5.0 (2020-08-06)

#### :rocket: New Feature

- Add async task manager
- Separate Gremlin query and task in data-analyze module, users can move to task details directly in history
- Display `deleting` status in schema configurations

<br />

## 1.3.1 (2020-07-28)

1.3.1 added support for data import from users by uploading their .csv files and setting data mappings.

Users can create jobs and manage load tasks with each. Details of Jobs that failed or succeed can be checked out.

#### :rocket: New Feature

- data-import support
- Job manager can handle different jobs, which could contain multiple load tasks

<br />

## 1.2.1 (2020-02-27)

1.2.1 added graph-mode view for metadata management. It supports all functions that list-mode has with better visual effect.

#### :rocket: New Feature

- graph-mode view in metadata management

<br />

## 2019-08-16

### Graph Management (front page)

#### :nail_care: Enhancement:

- Hover on text in GraphManamgentList will pop complete text

<br />

## 2019-08-15

### Graph Management (front page)

#### :bug: Bug Fix:

- Unexpected word break on navbar which happened in Safari
- Layout didn't vanish after the success request of creating/editing graph
- Empty value in username/password field would cause an error in front-end after click 'save' button
- Unexpected radio of width & word break in graph-list
- Hover & active state of primary button didn't show up

#### :nail_care: Enhancement:

- Change placeholders of username/password field in graph-creation form
- Change text in delete modal (confirmation to delete a graph)
- Change styles of title in Modal Component
- Normal/hover/active/disabled state of primary button now has a new series of background colors
- Fix the incorrect color of text in the left side of GraphManagementHeader
- Disable button for graph-creation before user closes the layout
- Add validation for host/port in form
- Cancel the linkage between highlighted words in list and values in search, the matched words should be highlighted after a search request
- GraphManagementList will reset to default if user clicks the reset button in search, and he has already searched a word before (which equals to refresh the page)
- Replace the favico with a new one.

