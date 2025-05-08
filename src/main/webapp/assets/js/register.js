const select = document.querySelector('select');
const explaination = document.querySelector('#explainationFormGroup');

function isOrga(){
    return select.value == "organisateur";
}

function hide(){
    explaination.classList.add('hide');
}

function show(){
    explaination.classList.remove('hide');
}

function displayRequestInput(){
    if(isOrga()){
        show();
        return
    }
    hide();
}

select.addEventListener('change', displayRequestInput)